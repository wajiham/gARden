package ch.argarden.app.features.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import ch.argarden.app.features.auth.SessionState
import ch.argarden.app.ui.*

@Composable fun ProfileScreen(state: SessionState, friendCount: Int?, onSave: (String) -> Unit,
    onLogout: () -> Unit, back: (() -> Unit)? = null, onRetry: () -> Unit) {
    var username by rememberSaveable(state.profile?.username) { mutableStateOf(state.profile?.username.orEmpty()) }
    GardenPage(if (state.profile == null) "Finish your profile" else "My profile", back) {
        if (state.profile == null) Text("Your account exists. Save a username to enter AR Garden.")
        Text("Email: ${state.account?.email.orEmpty()}")
        Text("User ID: ${state.account?.uid.orEmpty()}")
        Text("Friends: ${friendCount?.toString() ?: "Unavailable"}")
        OutlinedTextField(username, { username = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Status(state.busy || state.profileLoading, state.message)
        Action("Save username", !state.busy, onClick = { onSave(username) })
        if (state.message != null) TextButton(onClick = onRetry) { Text("Reload profile") }
        OutlinedButton(onClick = onLogout, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Logout") }
    }
}
