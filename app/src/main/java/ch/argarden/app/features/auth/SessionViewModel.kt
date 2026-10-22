package ch.argarden.app.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.argarden.app.data.model.*
import ch.argarden.app.data.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class SessionState(val initializing: Boolean = true, val account: Account? = null,
    val profile: User? = null, val profileLoading: Boolean = false, val busy: Boolean = false,
    val message: String? = null)

class SessionViewModel(private val auth: AuthRepository, private val profiles: ProfileRepository) : ViewModel() {
    private val mutable = MutableStateFlow(SessionState())
    val state = mutable.asStateFlow()
    private var profileJob: Job? = null
    init {
        viewModelScope.launch {
            auth.accounts.collect { account ->
                profileJob?.cancel()
                mutable.value = SessionState(initializing = false, account = account,
                    profileLoading = account != null, busy = mutable.value.busy)
                if (account != null) observeProfile(account.uid)
            }
        }
    }
    private fun observeProfile(uid: String) {
        profileJob = viewModelScope.launch {
            profiles.observe(uid).catch { error ->
                mutable.update { it.copy(profileLoading = false, message = error.userMessage()) }
            }.collect { profile -> mutable.update { it.copy(profile = profile, profileLoading = false) } }
        }
    }
    fun retryProfile() {
        val uid = state.value.account?.uid ?: return
        profileJob?.cancel()
        mutable.update { it.copy(profileLoading = true, message = null) }
        observeProfile(uid)
    }
    fun login(email: String, password: String) {
        val error = Validation.login(email, password)
        if (error != null) { message(error); return }
        operation { auth.login(email, password) }
    }
    fun register(username: String, email: String, password: String, confirm: String) {
        val error = Validation.registration(username, email, password, confirm)
        if (error != null) { message(error); return }
        operation {
            auth.register(email, password)
            val account = auth.accounts.filterNotNull().first()
            // Wait until the session listener has applied the account before publishing errors.
            state.first { it.account?.uid == account.uid }
            // Auth and Firestore cannot share a transaction. Missing profiles get a repair screen.
            profiles.saveUsername(account.uid, username)
        }
    }
    fun saveUsername(username: String) {
        val uid = state.value.account?.uid ?: return
        val error = Validation.username(username)
        if (error != null) { message(error); return }
        operation { profiles.saveUsername(uid, username); message("Username saved.") }
    }
    fun logout() = auth.logout()
    private fun message(value: String) { mutable.update { it.copy(message = value) } }
    private fun operation(block: suspend () -> Unit) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try { block() } catch (e: CancellationException) { throw e }
            catch (e: Exception) { message(e.userMessage()) }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
}

fun Throwable.userMessage(): String = when (this) {
    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Email or password is incorrect, or the credentials are invalid."
    is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "An account already uses this email. Try signing in."
    is com.google.firebase.FirebaseNetworkException -> "Could not connect. Check your internet connection and try again."
    is com.google.firebase.firestore.FirebaseFirestoreException -> when (code) {
        com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Access denied. Check the deployed Firestore rules and your sign-in."
        com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE -> "The server is unavailable. Reconnect and try again."
        else -> "The database operation failed. Please try again."
    }
    is IllegalArgumentException, is IllegalStateException -> message ?: "Unable to complete this action."
    else -> "Unable to complete this action. Please try again."
}
