package ch.argarden.app

import ch.argarden.app.data.model.*
import org.junit.Assert.*
import org.junit.Test

class ValidationTest {
    @Test fun malformedFriendshipsCannotReachTheUi() {
        val valid = FriendRequest("alice|bob", listOf("alice", "bob"), "alice", "bob")
        assertTrue(valid.isValidFor("alice"))
        assertFalse(valid.isValidFor("eve"))
        assertFalse(valid.copy(members = listOf("alice", "alice")).isValidFor("alice"))
        assertFalse(valid.copy(senderId = "bob").isValidFor("alice"))
        assertFalse(valid.copy(members = emptyList()).isValidFor("alice"))
        assertFalse(valid.copy(id = "wrong").isValidFor("alice"))
    }
    @Test fun registrationValidatesEveryField() {
        assertNull(Validation.registration("Alice", "alice@example.com", "password123", "password123"))
        assertNotNull(Validation.registration("", "alice@example.com", "password123", "password123"))
        assertNotNull(Validation.registration("Alice", "bad-email", "password123", "password123"))
        assertNotNull(Validation.registration("Alice", "alice@example.com", "short", "short"))
        assertNotNull(Validation.registration("Alice", "alice@example.com", "password123", "different"))
    }
    @Test fun usernameValidationAndNormalization() {
        assertEquals("alice_01", Validation.normalize("  ALIce_01  "))
        listOf("", "  ", "ab", "bad name", "a/b", "a|b", "a".repeat(25)).forEach { assertNotNull(Validation.username(it)) }
        listOf("abc", "Alice_01", "a".repeat(24)).forEach { assertNull(Validation.username(it)) }
    }
    @Test fun loginDoesNotImposeNewRegistrationPasswordRules() {
        assertNull(Validation.login("a@b.com", "oldpass"))
        assertNotNull(Validation.login("a@b.com", ""))
        assertNotNull(Validation.login("invalid", "password"))
    }
    @Test fun pairsAreCanonicalAndCannotBeSelfRequests() {
        assertEquals(FriendshipPolicy.pairId("alice", "bob"), FriendshipPolicy.pairId("bob", "alice"))
        assertThrows(IllegalArgumentException::class.java) { FriendshipPolicy.pairId("alice", "alice") }
    }
    @Test fun onlyRecipientAcceptsAndOnlyMembersRemove() {
        val pending = FriendRequest("alice|bob", listOf("alice", "bob"), "alice", "bob")
        assertTrue(FriendshipPolicy.canAccept(pending, "bob"))
        assertFalse(FriendshipPolicy.canAccept(pending, "alice"))
        assertFalse(FriendshipPolicy.canRemove(pending, "bob"))
        val accepted = pending.copy(status = RequestStatus.ACCEPTED)
        assertFalse(FriendshipPolicy.canAccept(accepted, "bob"))
        assertTrue(FriendshipPolicy.canRemove(accepted, "alice"))
        assertTrue(FriendshipPolicy.canRemove(accepted, "bob"))
        assertFalse(FriendshipPolicy.canRemove(accepted, "eve"))
    }
    @Test fun duplicatePendingAndAcceptedRequestsAreRejected() {
        FriendshipPolicy.requireNew(null)
        assertThrows(IllegalStateException::class.java) { FriendshipPolicy.requireNew(FriendRequest()) }
        assertThrows(IllegalStateException::class.java) { FriendshipPolicy.requireNew(FriendRequest(status = RequestStatus.ACCEPTED)) }
    }
}
