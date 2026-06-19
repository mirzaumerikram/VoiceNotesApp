package com.example.voicenotes.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vn_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_NAME = "user_name"
        private const val KEY_EMAIL = "user_email"
    }

    fun login(name: String, email: String) {
        prefs.edit().apply {
            putBoolean(KEY_LOGGED_IN, true)
            putString(KEY_NAME, name)
            putString(KEY_EMAIL, email)
            apply()
        }
    }

    fun logout() = prefs.edit().clear().apply()

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)

    fun getUserName(): String = prefs.getString(KEY_NAME, "User") ?: "User"

    fun getUserEmail(): String = prefs.getString(KEY_EMAIL, "") ?: ""

    // Store registered users as email=name|password pairs
    fun registerUser(name: String, email: String, password: String) {
        prefs.edit().putString("user_$email", "$name|$password").apply()
    }

    fun validateLogin(email: String, password: String): String? {
        // Demo account
        if (email == "demo@voicenotes.com" && password == "password123") return "Demo User"
        val stored = prefs.getString("user_$email", null) ?: return null
        val parts = stored.split("|")
        return if (parts.size == 2 && parts[1] == password) parts[0] else null
    }

    fun emailExists(email: String): Boolean =
        prefs.contains("user_$email") || email == "demo@voicenotes.com"
}
