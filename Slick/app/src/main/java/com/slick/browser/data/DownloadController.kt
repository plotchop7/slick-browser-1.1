package com.slick.browser.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.URLUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadController(
    context: Context,
    private val scope: CoroutineScope,
    private val onChanged: (List<DownloadEntry>) -> Unit
) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(DownloadManager::class.java)
    private var watcher: Job? = null
    private val entries = linkedMapOf<Long, DownloadEntry>()

    fun enqueue(url: String, userAgent: String, contentDisposition: String?, mimeType: String?, path: String) {
        val name = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(name)
            .setDescription("Slick")
            .setMimeType(mimeType)
            .addRequestHeader("User-Agent", userAgent)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        val relative = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "$path/$name")
            "${Environment.DIRECTORY_DOWNLOADS}/$path/$name"
        } else {
            val directory = appContext.getExternalFilesDir(path) ?: appContext.filesDir
            request.setDestinationUri(Uri.withAppendedPath(Uri.fromFile(directory), name))
            "${directory.absolutePath}/$name"
        }
        val id = manager.enqueue(request)
        entries[id] = DownloadEntry(id, name, url, relative)
        emit()
        startWatcher()
    }

    fun refresh() {
        val ids = entries.keys.toLongArray()
        if (ids.isEmpty()) {
            onChanged(emptyList())
            return
        }
        scope.launch(Dispatchers.IO) {
            val updated = mutableListOf<DownloadEntry>()
            ids.forEach { id ->
                manager.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
                    if (cursor.moveToFirst()) {
                        val old = entries[id] ?: return@use
                        updated += old.copy(
                            downloadedBytes = cursor.long(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
                            totalBytes = cursor.long(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
                            status = cursor.int(DownloadManager.COLUMN_STATUS)
                        )
                    }
                }
            }
            updated.forEach { entries[it.id] = it }
            withContext(Dispatchers.Main) { emit() }
        }
    }

    private fun startWatcher() {
        if (watcher?.isActive == true) return
        watcher = scope.launch {
            while (isActive) {
                refresh()
                delay(450)
                val active = entries.values.any {
                    it.status == DownloadManager.STATUS_RUNNING || it.status == DownloadManager.STATUS_PENDING
                }
                if (!active && entries.isNotEmpty()) break
            }
        }
    }

    private fun emit() = onChanged(entries.values.sortedByDescending { it.id })

    private fun android.database.Cursor.long(column: String): Long =
        getColumnIndex(column).takeIf { it >= 0 }?.let(::getLong) ?: 0L

    private fun android.database.Cursor.int(column: String): Int =
        getColumnIndex(column).takeIf { it >= 0 }?.let(::getInt) ?: 0
}
