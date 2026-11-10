package com.example.nativestarter.data

import android.content.Context

/**
 * Persistence for the theme preference. SharedPreferences on purpose: one boolean,
 * no need for a DataStore dependency stack.
 */
object ThemePreferences {

    private const val PREFS_NAME = "nativestarter_prefs"
    private const val KEY_DARK_THEME = "dark_theme"

    fun load(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_THEME, false)
    }

    fun save(context: Context, darkTheme: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_THEME, darkTheme).apply()
    }
}
