package ch.argarden.app.navigation

import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import ch.argarden.app.data.model.RequestStatus
import ch.argarden.app.features.auth.*
import ch.argarden.app.features.profile.ProfileScreen
import ch.argarden.app.features.social.*
import ch.argarden.app.features.garden.*
import ch.argarden.app.features.watering.WateringScreen
import ch.argarden.app.ui.*

@Composable fun AuthNavigation(state: SessionState, vm: SessionViewModel) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "login") {
        composable("login") { AuthScreen(state, false, { nav.navigate("register") }, vm::login, vm::register) }
        composable("register") { AuthScreen(state, true, { nav.popBackStack() }, vm::login, vm::register) }
    }
}

@Composable fun GardenNavigation(session: SessionState, social: SocialState, onSave: (String) -> Unit,
    onLogout: () -> Unit, onRetryProfile: () -> Unit, socialVm: SocialViewModel) {
    val nav = rememberNavController()
    val back: () -> Unit = { nav.popBackStack(); Unit }
    NavHost(nav, startDestination = "home") {
        composable("home") {
            GardenPage("Welcome, ${session.profile?.username.orEmpty()}") {
                Text("A little space to grow, together.")
                Action("My Profile") { nav.navigate("profile") }
                Action("Friends") { nav.navigate("friends") }
                Action("My Garden") { nav.navigate("garden") }
                Action("Watering") { nav.navigate("watering") }
            }
        }
        composable("profile") {
            ProfileScreen(session, if (social.loading) null else social.relationships.count { it.status == RequestStatus.ACCEPTED },
                onSave, onLogout, back, onRetryProfile)
        }
        composable("friends") {
            FriendsScreen(social, socialVm.uid, socialVm::search, socialVm::send, socialVm::accept,
                socialVm::reject, socialVm::remove, { nav.navigate("friend/${Uri.encode(it)}") }, socialVm::retry, back)
        }
        composable("garden") { MyGardenScreen(back) }
        composable("watering") { WateringScreen(back) }
        composable("friend/{uid}", arguments = listOf(navArgument("uid") { type = NavType.StringType })) {
            val uid = requireNotNull(it.arguments?.getString("uid"))
            FriendGardenScreen(uid, social.users[uid]?.username,
                social.relationships.any { r -> uid in r.members && r.status == RequestStatus.ACCEPTED }, back)
        }
    }
}
