package ch.argarden.app.features.garden

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import ch.argarden.app.ui.GardenPage

@Composable fun MyGardenScreen(back: () -> Unit) {
    GardenPage("My garden", back) { Text("AR Garden — Coming in Part 2") }
}
/** Pass the stable owner UID to Member 2's read-only garden repository, never the username. */
@Composable fun FriendGardenScreen(ownerId: String, username: String?, isFriend: Boolean, back: () -> Unit) {
    GardenPage("Friend's garden", back) {
        if (isFriend) {
            Text("${username ?: "Your friend"}'s garden")
            Text("Owner ID: $ownerId")
            Text("Saved garden preview — Coming in Part 2")
            Text("Read-only visit. No garden data is loaded in Part 1.")
        } else Text("This friendship is no longer available. Return to your friends list.")
    }
}
