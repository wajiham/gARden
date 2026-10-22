package ch.argarden.app.data.model

/** Public information only. Email is read from the signed-in Firebase Auth account. */
data class User(val uid: String = "", val username: String = "", val usernameNormalized: String = "")
data class Account(val uid: String, val email: String)
enum class RequestStatus { PENDING, ACCEPTED }
/** One canonical document represents both a pending request and an accepted friendship. */
data class FriendRequest(
    val id: String = "", val members: List<String> = emptyList(),
    val senderId: String = "", val recipientId: String = "",
    val status: RequestStatus = RequestStatus.PENDING
) {
    fun other(uid: String): String = members.first { it != uid }
    fun isValidFor(uid: String): Boolean = members.size == 2 && members.distinct().size == 2 &&
        uid in members && senderId in members && recipientId in members && senderId != recipientId &&
        id == members.joinToString("|")
}

// Integration contracts only: Part 1 never persists or renders these models.
// Members 2 and 3 must agree on units, schema versioning and access rules before storage.
data class Garden(val id: String, val ownerId: String, val plants: List<GardenPlant> = emptyList())
/** Position in metres; rotation in degrees; scale is a multiplier. No AR anchor is assumed. */
data class GardenPlant(val id: String, val speciesId: String, val x: Float = 0f,
    val y: Float = 0f, val z: Float = 0f, val rotationDegrees: Float = 0f, val scale: Float = 1f)
data class PlantSpecies(val id: String, val name: String, val modelAsset: String)
data class WaterTank(val ownerId: String, val capacityMl: Int, val remainingMl: Int)
