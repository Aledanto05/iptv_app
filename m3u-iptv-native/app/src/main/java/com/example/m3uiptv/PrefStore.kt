package com.example.m3uiptv

import android.content.Context

class PrefStore(context: Context) {
    private val prefs = context.getSharedPreferences("m3u_iptv_prefs", Context.MODE_PRIVATE)

    fun saveStrings(key: String, values: Set<String>) {
        prefs.edit().putStringSet(key, values).apply()
    }

    fun getStrings(key: String): MutableSet<String> {
        return prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    fun saveText(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getText(key: String): String {
        return prefs.getString(key, "") ?: ""
    }
}
