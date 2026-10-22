package ch.argarden.app.features.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.argarden.app.data.model.*
import ch.argarden.app.data.repository.*
import ch.argarden.app.features.auth.userMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class SocialState(val loading: Boolean = true, val busy: Boolean = false,
    val relationships: List<FriendRequest> = emptyList(), val users: Map<String, User> = emptyMap(),
    val results: List<User> = emptyList(), val searched: Boolean = false, val message: String? = null)
class SocialViewModel(val uid: String, private val social: SocialRepository,
    private val profiles: ProfileRepository) : ViewModel() {
    private val mutable = MutableStateFlow(SocialState())
    val state = mutable.asStateFlow()
    private var listener: Job? = null
    init { retry() }
    fun retry() {
        listener?.cancel()
        mutable.update { it.copy(loading = true, message = null) }
        listener = viewModelScope.launch {
            social.observe(uid).catch { error ->
                mutable.update { it.copy(loading = false, message = error.userMessage()) }
            }.collectLatest { relationships ->
                mutable.update { it.copy(loading = false, relationships = relationships, users = emptyMap()) }
                coroutineScope {
                    relationships.map { it.other(uid) }.distinct().forEach { other ->
                        launch {
                            profiles.observe(other).catch { error ->
                                mutable.update { it.copy(message = error.userMessage()) }
                            }.collect { user ->
                                if (user != null) mutable.update { it.copy(users = it.users + (other to user)) }
                            }
                        }
                    }
                }
            }
        }
    }
    fun search(query: String) = operation {
        require(Validation.username(query) == null) { Validation.username(query).orEmpty() }
        mutable.update { it.copy(results = emptyList(), searched = false) }
        val users = profiles.search(Validation.normalize(query)).filter { it.uid != uid }
        mutable.update { it.copy(results = users, searched = true) }
    }
    fun send(other: String) = operation { social.send(uid, other); message("Friend request sent.") }
    fun accept(id: String) = operation { social.accept(uid, id); message("Friend request accepted.") }
    fun reject(id: String) = operation { social.reject(uid, id); message("Friend request rejected.") }
    fun remove(id: String) = operation { social.remove(uid, id); message("Friend removed.") }
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
