package com.clenova.app

import android.Manifest
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.media3.common.util.UnstableApi
import com.clenova.app.data.storage.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.DataOutputStream
import java.util.UUID

/** Uses disposable, app-owned fixtures only. Never sends a Trash intent or deletes existing user media. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 33)
@androidx.annotation.OptIn(UnstableApi::class)
class CleanDeviceSmokeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val resolver = context.contentResolver
    private val created = mutableListOf<Uri>()
    private val token = UUID.randomUUID().toString().take(8)

    private fun createMedia(video: Boolean, name: String, folder: String, write: (java.io.OutputStream) -> Unit): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, if (video) "video/mp4" else "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = if (video) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = requireNotNull(resolver.insert(collection, values))
        created.add(uri)
        requireNotNull(resolver.openOutputStream(uri, "w")).use(write)
        assertEquals(1, resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null))
        return uri
    }

    private fun photo(name: String, shift: Int, mode: Int = 0, screenshot: Boolean = false): Uri =
        createMedia(false, name, if (screenshot) "Pictures/Screenshots/" else "Pictures/ClenovaQA/") { stream ->
            val bmp = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
            try {
                for (y in 0 until 128) for (x in 0 until 128) {
                    val v = 30 + (x + y) / 3
                    val color = when (mode) {
                        1 -> Color.rgb(20 + x / 2, 160 + y / 4, 30)
                        2 -> Color.rgb(150 + y / 3, 30 + x / 4, 100)
                        else -> Color.rgb(v + shift, v + shift, v + shift)
                    }
                    bmp.setPixel(x, y, color)
                }
                assertTrue(bmp.compress(Bitmap.CompressFormat.PNG, 100, stream))
            } finally { bmp.recycle() }
        }

    private fun assertNotTrashed(uri: Uri) {
        resolver.query(uri, arrayOf(MediaStore.MediaColumns.IS_TRASHED), null, null, null).use {
            assertNotNull(it); assertTrue(it!!.moveToFirst()); assertEquals(0, it.getInt(0))
        }
    }

    @Test fun scanReviewAndCompressRealFixtures() = runBlocking {
        // This test is intended for a fresh CI emulator, not a personal gallery.
        org.junit.Assume.assumeTrue(Build.FINGERPRINT.contains("generic") || Build.FINGERPRINT.contains("emulator") || Build.MODEL.contains("sdk", true))
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.READ_MEDIA_IMAGES)
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.READ_MEDIA_VIDEO)
        try {
            val dup1 = photo("QA_${token}_a.png", 0, 1)
            val dup2 = photo("QA_${token}_b.png", 0, 1)
            val near1 = photo("QA_${token}_near1.png", 0)
            val near2 = photo("QA_${token}_near2.png", 6)
            val shot = photo("Screenshot_${token}.png", 0, 2, true)
            val video = createMedia(true, "QA_${token}.mp4", "Movies/ClenovaQA/") { stream ->
                instrumentation.context.assets.open("clean-qa-source.mp4").use { it.copyTo(stream) }
            }
            val large = createMedia(true, "QA_${token}_large.mp4", "Movies/ClenovaQA/") { stream ->
                instrumentation.context.assets.open("clean-qa-source.mp4").use { it.copyTo(stream) }
                // A valid MP4 free box creates a large fixture without long encoding time.
                val data = DataOutputStream(stream)
                val padding = 100_000_000
                data.writeInt(padding + 8)
                data.writeBytes("free")
                val zeros = ByteArray(100_000)
                repeat(padding / zeros.size) { data.write(zeros) }
                data.flush()
            }
            val scanner = MediaScanner(context)
            val result = withTimeout(120_000) {
                var snapshot = scanner.scan { _, _, _ -> }
                var attempts = 0
                while ((!snapshot.photos.any { it.uri == shot } || !snapshot.largeVideos.any { it.uri == large }) && attempts++ < 3) {
                    delay(1000)
                    snapshot = scanner.scan { _, _, _ -> }
                }
                snapshot
            }
            assertTrue(result.photos.any { it.uri == shot })
            val duplicates = result.duplicateGroups.first { group -> group.map { it.uri }.containsAll(listOf(dup1, dup2)) }
            val duplicateSelection = setOf(duplicates[1].uri)
            DuplicateTrash.request(context, result, duplicateSelection)
            val similar = result.similarGroups.first { group -> group.map { it.uri }.containsAll(listOf(near1, near2)) }
            DuplicateTrash.request(context, result, setOf(similar[1].uri), similar = true)
            assertTrue(result.screenshots.any { it.uri == shot })
            ScreenshotTrash.request(context, result, setOf(shot))
            assertTrue(result.largeVideos.any { it.uri == large })
            assertTrue(result.largeFiles.any { it.uri == large })
            LargeMediaTrash.request(context, result, setOf(large), true)
            LargeMediaTrash.request(context, result, setOf(large), false)
            SwipeTrash.request(context, result, setOf(shot))
            created.forEach(::assertNotTrashed)
            val input = VideoCompression.inspect(context, video)
            val output = withTimeout(120_000) { VideoCompression.encode(context, input, VideoPreset.SMALL) {} }
            try {
                assertTrue(output.bytes > 0)
                assertTrue("Fixture should compress to a smaller file", output.bytes < input.bytes)
                val saved = VideoCompression.save(context, output, input.bytes)
                created.add(saved)
                val savedSource = VideoCompression.inspect(context, saved)
                assertEquals(output.bytes, savedSource.bytes)
                assertEquals(input.bytes, VideoCompression.inspect(context, video).bytes)
                assertTrue(CleanupHistory(context).records().any { it.kind == CleanupKind.COMPRESS && it.bytes == output.bytes && it.originalBytes == input.bytes })
            } finally { output.file.delete() }
        } finally {
            created.forEach { runCatching { resolver.delete(it, null, null) } }
        }
    }
}
