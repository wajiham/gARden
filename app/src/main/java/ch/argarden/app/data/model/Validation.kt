package ch.argarden.app.data.model

import java.util.Locale

object Validation {
    fun normalize(username: String) = username.trim().lowercase(Locale.ROOT)
    fun username(value: String): String? = if (Regex("[A-Za-z0-9_]{3,24}").matches(value.trim())) null
        else "Use 3–24 letters, numbers or underscores."
    fun email(value: String): String? = if (Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(value.trim())) null
        else "Enter a valid email address."
    fun registration(username: String, email: String, password: String, confirm: String): String? =
        username(username) ?: email(email) ?: when {
            password.length < 8 -> "Use a password with at least 8 characters."
            password.length > 128 -> "Use a password with at most 128 characters."
            password != confirm -> "Passwords do not match."
            else -> null
        }
    fun login(email: String, password: String): String? = email(email)
        ?: if (password.isEmpty()) "Enter your password." else null
}

object FriendshipPolicy {
    // Firebase-generated email/password UIDs contain no '|'; rules enforce this too.
    fun pairId(a: String, b: String): String {
        require(a != b) { "You cannot add yourself." }
        require(a.isNotBlank() && b.isNotBlank() && '|' !in a && '|' !in b)
        return listOf(a, b).sorted().joinToString("|")
    }
    fun canAccept(request: FriendRequest, uid: String) =
        request.status == RequestStatus.PENDING && request.recipientId == uid
    fun canRemove(request: FriendRequest, uid: String) =
        request.status == RequestStatus.ACCEPTED && uid in request.members
    fun requireNew(existing: FriendRequest?) {
        check(existing == null) { "A request or friendship already exists. Check your incoming requests." }
    }
}
