package com.clenova.app.data.storage

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.*
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.util.UUID

internal data class VideoSource(val uri: Uri, val name: String, val bytes: Long, val height: Int, val durationMs: Long, val bitrate: Long)
internal enum class VideoPreset(val maxHeight: Int, val maxBitrate: Int, val ratio: Double) {
    HIGH(1080, 8_000_000, .75), BALANCED(720, 3_000_000, .5), SMALL(480, 1_200_000, .3);
    fun height(sourceHeight: Int): Int = (minOf(sourceHeight, maxHeight) / 2 * 2).coerceAtLeast(2)
    fun bitrate(sourceBitrate: Long): Int = (sourceBitrate * ratio).toLong().coerceIn(100_000, maxBitrate.toLong()).toInt()
}
internal data class EncodedVideo(val file: File, val bytes: Long)

@UnstableApi
internal object VideoCompression {
    suspend fun inspect(context: Context, uri: Uri): VideoSource = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var name = "video.mp4"
        var size = -1L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use {
            if (it.moveToFirst()) {
                name = it.getString(0) ?: name
                if (!it.isNull(1)) size = it.getLong(1)
            }
        }
        if (size <= 0) size = resolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
        require(size > 0) { "Unknown source size" }
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val bit = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
                ?: (size.toDouble() * 8000.0 / duration.coerceAtLeast(1)).toLong()
            require(duration > 0 && width >= 2 && height >= 2 && bit > 0)
            VideoSource(uri, name, size, if (rotation == 90 || rotation == 270) width else height, duration, bit)
        } finally { retriever.release() }
    }

    suspend fun encode(context: Context, source: VideoSource, preset: VideoPreset, progress: (Int?) -> Unit): EncodedVideo =
        withContext(Dispatchers.Main.immediate) {
            val output = File(context.cacheDir, "clenova-export-${UUID.randomUUID()}.mp4")
            val finished = CompletableDeferred<Unit>()
            var transformer: Transformer? = null
            var successful = false
            try {
                val effects = Effects(emptyList(), listOf(Presentation.createForHeight(preset.height(source.height))))
                val edited = EditedMediaItem.Builder(MediaItem.fromUri(source.uri)).setEffects(effects).build()
                val encoder = DefaultEncoderFactory.Builder(context)
                    .setRequestedVideoEncoderSettings(VideoEncoderSettings.Builder().setBitrate(preset.bitrate(source.bitrate)).build())
                    .build()
                val engine = Transformer.Builder(context).setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC).setEncoderFactory(encoder)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) { finished.complete(Unit) }
                        override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                            finished.completeExceptionally(exportException)
                        }
                    }).build()
                transformer = engine
                engine.start(edited, output.absolutePath)
                val holder = ProgressHolder()
                while (!finished.isCompleted) {
                    progress(if (engine.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) holder.progress.coerceIn(0, 99) else null)
                    delay(250)
                }
                finished.await()
                currentCoroutineContext().ensureActive()
                val size = output.length()
                require(size > 0)
                withContext(Dispatchers.IO) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(output.absolutePath)
                        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                        require(width > 0 && duration > 0 && kotlin.math.abs(duration - source.durationMs) <= maxOf(1000L, source.durationMs / 100)) { "Incomplete video output" }
                    } finally { retriever.release() }
                }
                successful = true
                progress(100)
                EncodedVideo(output, size)
            } finally {
                if (!finished.isCompleted) transformer?.cancel()
                if (!successful) output.delete()
            }
        }

    /** Publish only an explicitly approved, completed copy. Never open the original for writing. */
    suspend fun save(context: Context, output: EncodedVideo, originalBytes: Long): Uri = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < 29) throw UnsupportedOperationException("Android 10 required")
        require(output.file.isFile && output.file.length() == output.bytes && output.bytes in 1 until originalBytes)
        require(context.cacheDir.usableSpace >= output.bytes) { "Insufficient temporary storage" }
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "Clenova_${UUID.randomUUID()}.mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/Clenova")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: throw IOException("Cannot create output")
        var published = false
        try {
            var written = 0L
            output.file.inputStream().use { input ->
                (resolver.openOutputStream(uri, "w") ?: throw IOException("Cannot write output")).use { stream ->
                    val buffer = ByteArray(128 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        stream.write(buffer, 0, n)
                        written += n
                    }
                }
            }
            require(written == output.bytes) { "Incomplete output copy" }
            currentCoroutineContext().ensureActive()
            if (resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null) != 1)
                throw IOException("Cannot publish output")
            published = true
            CleanupHistory(context).record(CleanupKind.COMPRESS, 1, written, originalBytes)
            uri
        } finally { if (!published) runCatching { resolver.delete(uri, null, null) } }
    }
}
