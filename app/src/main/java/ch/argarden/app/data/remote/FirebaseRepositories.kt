package ch.argarden.app.data.remote

import ch.argarden.app.data.model.*
import ch.argarden.app.data.repository.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(private val auth: FirebaseAuth) : AuthRepository {
    override val accounts = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebase ->
            trySend(firebase.currentUser?.let { Account(it.uid, it.email.orEmpty()) })
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
    override suspend fun register(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
    }
    override suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }
    override fun logout() = auth.signOut()
}

class FirebaseProfileRepository(private val db: FirebaseFirestore) : ProfileRepository {
    override fun observe(uid: String): Flow<User?> = callbackFlow {
        val listener = db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) close(error) else trySend(snapshot?.toObject(User::class.java))
        }
        awaitClose { listener.remove() }
    }
    override suspend fun saveUsername(uid: String, username: String) {
        require(Validation.username(username) == null) { Validation.username(username).orEmpty() }
        db.collection("users").document(uid)
            .set(User(uid, username.trim(), Validation.normalize(username))).await()
    }
    override suspend fun search(normalizedUsername: String): List<User> = db.collection("users")
        .whereEqualTo("usernameNormalized", normalizedUsername).limit(20).get(Source.SERVER)
        .await().toObjects(User::class.java)
}

class FirebaseSocialRepository(private val db: FirebaseFirestore) : SocialRepository {
    private val relationships = db.collection("relationships")
    override fun observe(uid: String): Flow<List<FriendRequest>> = callbackFlow {
        val listener = relationships.whereArrayContains("members", uid).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) close(error)
            // Do not present an optimistic write as a server-approved friendship.
            // INCLUDE ensures the acknowledgement (or rollback) emits even if only metadata changes.
            else if (snapshot != null && !snapshot.metadata.hasPendingWrites()) {
                trySend(snapshot.documents.mapNotNull { it.requestOrNull() }.filter { it.isValidFor(uid) })
            }
        }
        awaitClose { listener.remove() }
    }
    override suspend fun send(uid: String, recipientId: String) {
        val id = FriendshipPolicy.pairId(uid, recipientId)
        val reference = relationships.document(id)
        db.runTransaction { tx ->
            val existing = tx.get(reference)
            FriendshipPolicy.requireNew(if (existing.exists()) existing.request() else null)
            check(tx.get(db.collection("users").document(recipientId)).exists()) { "User no longer exists." }
            tx.set(reference, mapOf("members" to listOf(uid, recipientId).sorted(),
                "senderId" to uid, "recipientId" to recipientId, "status" to "PENDING"))
        }.await()
    }
    override suspend fun accept(uid: String, requestId: String) = change(uid, requestId, "accept")
    override suspend fun reject(uid: String, requestId: String) = change(uid, requestId, "reject")
    override suspend fun remove(uid: String, requestId: String) = change(uid, requestId, "remove")
    private suspend fun change(uid: String, id: String, action: String) {
        val reference = relationships.document(id)
        db.runTransaction { tx ->
            val snapshot = tx.get(reference)
            check(snapshot.exists()) { "This request or friendship no longer exists." }
            val request = snapshot.request()
            when (action) {
                "accept" -> {
                    check(FriendshipPolicy.canAccept(request, uid)) { "Only the recipient can accept a pending request." }
                    tx.update(reference, "status", "ACCEPTED")
                }
                "reject" -> {
                    check(FriendshipPolicy.canAccept(request, uid)) { "Only the recipient can reject a pending request." }
                    tx.delete(reference)
                }
                else -> {
                    check(FriendshipPolicy.canRemove(request, uid)) { "This friendship cannot be removed." }
                    tx.delete(reference)
                }
            }
        }.await()
    }
    private fun DocumentSnapshot.requestOrNull(): FriendRequest? {
        val fields = data ?: return null
        val members = fields["members"] as? List<*> ?: return null
        if (members.any { it !is String }) return null
        val sender = fields["senderId"] as? String ?: return null
        val recipient = fields["recipientId"] as? String ?: return null
        val status = RequestStatus.entries.firstOrNull { it.name == fields["status"] } ?: return null
        return FriendRequest(id, members.filterIsInstance<String>(), sender, recipient, status)
            .takeIf { it.isValidFor(sender) }
    }
    private fun DocumentSnapshot.request() = checkNotNull(requestOrNull()) { "Invalid friendship data. Please contact the project maintainer." }
}
