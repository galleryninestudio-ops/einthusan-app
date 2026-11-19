package com.bench.einthusan.net

import org.json.JSONObject

sealed interface LoginResult {
    data class Ok(val premium: Boolean) : LoginResult
    data class Fail(val message: String) : LoginResult
    data object NoNetwork : LoginResult
}

/**
 * POST /ajax/login/?lang=tamil with xEvent=Login.
 * Verified against the live endpoint: bad credentials answer
 * {"Event":"UserMessage","Data":{"Err":true,"KeyCode":"user-not-found",...}}.
 */
class Auth(private val store: SecureStore) {

    init {
        // Older builds saved a password but never used it to restore a session.
        store.remove(KEY_PASS)
    }

    fun savedEmail(): String? = store.get(KEY_EMAIL)

    suspend fun login(email: String, password: String): LoginResult {
        val payload = JSONObject()
            .put("Email", email)
            .put("Password", password)
            .toString()

        val res = Net.ajax("/login/?lang=tamil", "Login", payload)
            ?: return LoginResult.NoNetwork

        val event = res.optString("Event")
        val data = res.optJSONObject("Data") ?: JSONObject()

        if (data.optBoolean("Err")) {
            return LoginResult.Fail(data.optString("Message").ifBlank { "Sign in failed." })
        }
        if (event == "UserMessage") {
            return LoginResult.Fail(data.optString("Message").ifBlank { "Sign in failed." })
        }

        store.put(KEY_EMAIL, email)
        val premium = premium()
        return LoginResult.Ok(premium)
    }

    /** data-premium lands on the root of every page once the session cookie exists. */
    suspend fun premium(): Boolean {
        val html = Net.get("/movie/browse/?lang=tamil") ?: return false
        return Regex("""data-premium="(true|false)"""").find(html)?.groupValues?.get(1) == "true"
    }

    fun signOut() {
        store.remove(KEY_EMAIL)
        store.remove(KEY_PASS)
        Net.jar.clear()
    }

    private companion object {
        const val KEY_EMAIL = "login.email"
        const val KEY_PASS = "login.password"
    }
}
