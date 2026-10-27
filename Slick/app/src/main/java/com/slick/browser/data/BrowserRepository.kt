package com.slick.browser.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject

class BrowserRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("slick_browser_v1", Context.MODE_PRIVATE)

    var searchEngine: SearchEngine
        get() = enumValueOr(prefs.getString("search_engine", null), SearchEngine.DUCKGO)
        set(value) = prefs.edit().putString("search_engine", value.name).apply()

    var monochrome: Boolean
        get() = prefs.getBoolean("monochrome", true)
        set(value) = prefs.edit().putBoolean("monochrome", value).apply()

    var theme: BrowserTheme
        get() = enumValueOr(prefs.getString("theme", null), BrowserTheme.BLACK)
        set(value) = prefs.edit().putString("theme", value.name).apply()

    var liquidMode: LiquidMode
        get() = enumValueOr(prefs.getString("liquid_mode", null), LiquidMode.LIQUID)
        set(value) = prefs.edit().putString("liquid_mode", value.name).apply()

    var energySaver: Boolean
        get() = prefs.getBoolean("energy_saver", false)
        set(value) = prefs.edit().putBoolean("energy_saver", value).apply()

    var downloadPath: String
        get() = prefs.getString("download_path", "Slick")?.trim()?.trim('/').orEmpty()
            .ifBlank { "Slick" }
        set(value) {
            val safe = value.trim().trim('/').replace(Regex("[\\\\:*?\"<>|]"), "_")
            prefs.edit().putString("download_path", safe.ifBlank { "Slick" }).apply()
        }

    fun bookmarks(): List<Bookmark> = parseList("bookmarks") { item ->
        Bookmark(
            id = item.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
            title = item.optString("title"),
            url = item.optString("url")
        )
    }.filter { it.url.isNotBlank() }

    fun saveBookmarks(items: List<Bookmark>) = saveList("bookmarks", items) { item ->
        JSONObject().put("id", item.id).put("title", item.title).put("url", item.url)
    }

    fun history(): List<HistoryEntry> = parseList("history") { item ->
        HistoryEntry(
            title = item.optString("title"),
            url = item.optString("url"),
            visitedAt = item.optLong("visitedAt")
        )
    }.filter { it.url.isNotBlank() }

    fun addHistory(entry: HistoryEntry) {
        val updated = buildList {
            add(entry)
            history().filterNot { it.url == entry.url }.take(999).forEach(::add)
        }
        saveList("history", updated) { item ->
            JSONObject()
                .put("title", item.title)
                .put("url", item.url)
                .put("visitedAt", item.visitedAt)
        }
    }

    fun clearHistory() = prefs.edit().remove("history").apply()

    fun absoluteDownloadDirectory(): String =
        "${Environment.DIRECTORY_DOWNLOADS}/$downloadPath"

    private fun <T> parseList(key: String, mapper: (JSONObject) -> T): List<T> = try {
        val array = JSONArray(prefs.getString(key, "[]").orEmpty())
        buildList {
            for (index in 0 until array.length()) add(mapper(array.getJSONObject(index)))
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun <T> saveList(key: String, items: List<T>, mapper: (T) -> JSONObject) {
        val array = JSONArray()
        items.forEach { array.put(mapper(it)) }
        prefs.edit().putString(key, array.toString()).apply()
    }

    private inline fun <reified T : Enum<T>> enumValueOr(value: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback
}
