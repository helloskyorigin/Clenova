package com.clenova.app.data.storage

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.os.Build
import org.json.JSONObject
import java.io.IOException

/** Local deny-list for CLENOVA cleanup requests. Never writes or moves source files. */
internal class MemoryGuardStore(private val context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("clenova_memory_guard", Context.MODE_PRIVATE)

    fun records(): Map<Uri, String> = synchronized(lock) {
        // Corrupt protection state must fail closed rather than silently allow cleanup.
        val json = JSONObject(prefs.getString("items", "{}") ?: "{}")
        json.keys().asSequence().associate { Uri.parse(it) to json.getString(it) }
    }

    fun add(items: List<ScannedMedia>): Boolean = synchronized(lock) {
        require(items.all { it.uri.authority == MediaStore.AUTHORITY })
        val updated = records() + items.associate { it.uri to it.name }
        require(updated.size <= LIMIT)
        save(updated)
    }

    fun remove(uri: Uri): Boolean = synchronized(lock) { save(records() - uri) }

    private fun save(items: Map<Uri, String>): Boolean {
        val json = JSONObject()
        items.forEach { (uri, name) -> json.put(uri.toString(), name) }
        return prefs.edit().putString("items", json.toString()).commit()
    }

    fun albums(): Set<String> = synchronized(lock) {
        val json = org.json.JSONArray(prefs.getString("albums", "[]") ?: "[]")
        (0 until json.length()).map { json.getString(it) }.toSet()
    }

    fun addAlbum(key: String): Boolean = synchronized(lock) {
        require(key.substringBefore("|").isNotBlank() && key.substringAfter("|", "").isNotBlank())
        val next = albums() + key
        require(next.size <= 200)
        prefs.edit().putString("albums", org.json.JSONArray(next.toList()).toString()).commit()
    }

    fun removeAlbum(key: String): Boolean = synchronized(lock) {
        prefs.edit().putString("albums", org.json.JSONArray((albums() - key).toList()).toString()).commit()
    }

    fun protectedUris(items: List<ScannedMedia>): Set<Uri> = synchronized(lock) {
        val keys = albums()
        records().keys + items.filter { albumKey(it) in keys }.map { it.uri }
    }

    fun ensureUnprotected(uris: Collection<Uri>) = synchronized(lock) {
        val selected = records()
        val folders = albums()
        for (uri in uris) {
            if (uri in selected) throw IOException("Memory Guard protected item")
            if (folders.isEmpty()) continue
            if (Build.VERSION.SDK_INT < 29) throw IOException("Cannot verify album protection")
            val folder = context.applicationContext.contentResolver.query(uri,
                arrayOf(MediaStore.MediaColumns.RELATIVE_PATH), null, null, null)?.use {
                if (!it.moveToFirst()) null else it.getString(0)
            } ?: throw IOException("Cannot verify album protection")
            if (albumKey(uri, folder) in folders) throw IOException("Memory Guard protected album")
        }
    }

    companion object {
        private val lock = Any()
        const val LIMIT = 1000
        fun albumKey(item: ScannedMedia): String? = albumKey(item.uri, item.folder)
        fun albumKey(uri: Uri, folder: String): String? {
            val volume = uri.pathSegments.firstOrNull() ?: return null
            val path = folder.trim().trim('/')
            return if (uri.authority == MediaStore.AUTHORITY && path.isNotBlank() && !folder.startsWith('/')) "$volume|$path" else null
        }
        fun albumLabel(key: String): String = key.substringAfter('|').substringAfterLast('/')
    }
}
