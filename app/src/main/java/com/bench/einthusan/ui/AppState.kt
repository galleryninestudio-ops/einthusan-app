package com.bench.einthusan.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bench.einthusan.data.Library
import com.bench.einthusan.data.Repo
import com.bench.einthusan.data.Settings
import com.bench.einthusan.net.Auth
import com.bench.einthusan.net.SecureStore

class AppState(context: Context) {

    val library = Library(context)
    val settings = Settings(context)
    private val secure = SecureStore(context)
    val auth = Auth(secure)

    var premium by mutableStateOf(library.premiumKnown)
        private set

    var email by mutableStateOf(auth.savedEmail())
        private set

    init {
        Repo.premium = premium
    }

    fun applyPremium(v: Boolean) {
        premium = v
        Repo.premium = v
        library.premiumKnown = v
    }

    fun applyEmail(v: String?) {
        email = v
    }

    /** Request Ultra HD when the account and title support it. */
    fun wantsUhd(uhdTitle: Boolean): Boolean = premium && uhdTitle

    fun signOut() {
        auth.signOut()
        Repo.clearWatchCache()
        applyPremium(false)
        applyEmail(null)
    }
}
