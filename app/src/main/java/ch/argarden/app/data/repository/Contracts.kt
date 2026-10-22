package ch.argarden.app.data.repository

import ch.argarden.app.data.model.*
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val accounts: Flow<Account?>
    suspend fun register(email: String, password: String)
    suspend fun login(email: String, password: String)
    fun logout()
}
interface ProfileRepository {
    fun observe(uid: String): Flow<User?>
    suspend fun saveUsername(uid: String, username: String)
    suspend fun search(normalizedUsername: String): List<User>
}
interface SocialRepository {
    fun observe(uid: String): Flow<List<FriendRequest>>
    suspend fun send(uid: String, recipientId: String)
    suspend fun accept(uid: String, requestId: String)
    suspend fun reject(uid: String, requestId: String)
    suspend fun remove(uid: String, requestId: String)
}
