package com.clenova.app.data.storage

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import org.json.JSONObject
import java.io.IOException

/** Local deny-list for CLENOVA cleanup requests. Never writes or moves source files. */
internal class MemoryGuardStore(context: Context) {
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

    fun ensureUnprotected(uris: Collection<Uri>) {
        if (uris.any { it in records() }) throw IOException("Memory Guard protected item")
    }

    companion object {
        private val lock = Any()
        const val LIMIT = 1000
    }
}
