package ch.argarden.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ch.argarden.app.data.model.*
import ch.argarden.app.data.repository.*
import ch.argarden.app.features.auth.SessionState
import ch.argarden.app.features.social.*
import ch.argarden.app.navigation.GardenNavigation
import ch.argarden.app.ui.theme.GardenTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class NavigationTest {
    @get:Rule val compose = createComposeRule()
    @Test fun dashboardPlaceholdersAndReadOnlyFriendRoute() {
        val request = FriendRequest("alice|bob", listOf("alice", "bob"), "alice", "bob", RequestStatus.ACCEPTED)
        val profiles = object : ProfileRepository {
            override fun observe(uid: String) = flowOf(User(uid, "Bob", "bob"))
            override suspend fun saveUsername(uid: String, username: String) = Unit
            override suspend fun search(normalizedUsername: String) = emptyList<User>()
        }
        val repository = object : SocialRepository {
            override fun observe(uid: String) = flowOf(listOf(request))
            override suspend fun send(uid: String, recipientId: String) = Unit
            override suspend fun accept(uid: String, requestId: String) = Unit
            override suspend fun reject(uid: String, requestId: String) = Unit
            override suspend fun remove(uid: String, requestId: String) = Unit
        }
        lateinit var vm: SocialViewModel
        compose.runOnUiThread { vm = SocialViewModel("alice", repository, profiles) }
        compose.setContent {
            GardenTheme {
                GardenNavigation(SessionState(false, Account("alice", "a@example.com"), User("alice", "Alice", "alice")),
                    SocialState(loading = false, relationships = listOf(request), users = mapOf("bob" to User("bob", "Bob", "bob"))),
                    {}, {}, {}, vm)
            }
        }
        compose.onNodeWithText("My Garden").performClick()
        compose.onNodeWithText("AR Garden — Coming in Part 2").assertIsDisplayed()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Watering").performClick()
        compose.onNodeWithText("Watering System — Coming in Part 3").assertIsDisplayed()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Friends").performClick()
        compose.onNodeWithText("Visit Garden").performScrollTo().performClick()
        compose.onNodeWithText("Bob's garden").assertIsDisplayed()
        compose.onNodeWithText("Read-only visit. No garden data is loaded in Part 1.").assertIsDisplayed()
        compose.runOnUiThread { androidx.lifecycle.ViewModelStore().also { it.put("test", vm); it.clear() } }
    }
}
