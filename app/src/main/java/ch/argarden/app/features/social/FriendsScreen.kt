package ch.argarden.app.features.social

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.argarden.app.data.model.*
import ch.argarden.app.ui.*

@Composable fun FriendsScreen(state: SocialState, uid: String, onSearch: (String) -> Unit,
    onSend: (String) -> Unit, onAccept: (String) -> Unit, onReject: (String) -> Unit,
    onRemove: (String) -> Unit, onVisit: (String) -> Unit, onRetry: () -> Unit, back: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var removing by remember { mutableStateOf<FriendRequest?>(null) }
    GardenPage("Grow your circle", back) {
        OutlinedTextField(query, { query = it }, label = { Text("Exact username") }, singleLine = true,
            supportingText = { Text("Case-insensitive. Results exclude your own account; up to 20 matches.") }, modifier = Modifier.fillMaxWidth())
        Action("Search", !state.busy) { onSearch(query) }
        Status(state.loading || state.busy, state.message)
        if (state.message != null) TextButton(onClick = onRetry) { Text("Refresh connections") }
        if (state.searched && state.results.isEmpty()) Text("No matching users.")
        state.results.forEach { user ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(user.username, style = MaterialTheme.typography.titleMedium)
                    Text("User ID: ${user.uid}", style = MaterialTheme.typography.bodySmall)
                    val existing = state.relationships.firstOrNull { user.uid in it.members }
                    if (existing == null) Action("Send request", !state.busy) { onSend(user.uid) }
                    else Text(if (existing.status == RequestStatus.ACCEPTED) "Already friends" else "Request pending")
                }
            }
        }
        Text("Incoming requests", style = MaterialTheme.typography.titleLarge)
        val incoming = state.relationships.filter { it.recipientId == uid && it.status == RequestStatus.PENDING }
        if (!state.loading && incoming.isEmpty()) Text("No invitations yet.")
        incoming.forEach { request ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.users[request.senderId]?.username ?: request.senderId)
                    Action("Accept", !state.busy) { onAccept(request.id) }
                    OutlinedButton(onClick = { onReject(request.id) }, enabled = !state.busy) { Text("Reject") }
                }
            }
        }
        Text("Sent requests", style = MaterialTheme.typography.titleLarge)
        val outgoing = state.relationships.filter { it.senderId == uid && it.status == RequestStatus.PENDING }
        if (!state.loading && outgoing.isEmpty()) Text("No pending sent requests.")
        outgoing.forEach { Text("${state.users[it.recipientId]?.username ?: it.recipientId} — Pending") }
        Text("Friends", style = MaterialTheme.typography.titleLarge)
        val friends = state.relationships.filter { it.status == RequestStatus.ACCEPTED }
        if (!state.loading && friends.isEmpty()) Text("Your friends will appear here after an invitation is accepted.")
        friends.forEach { request ->
            val other = request.other(uid)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.users[other]?.username ?: other, style = MaterialTheme.typography.titleMedium)
                    Action("Visit Garden") { onVisit(other) }
                    TextButton(onClick = { removing = request }, enabled = !state.busy) { Text("Remove friend") }
                }
            }
        }
    }
    removing?.let { request ->
        AlertDialog(onDismissRequest = { removing = null }, title = { Text("Remove friend?") },
            text = { Text("This removes the friendship for both accounts.") },
            confirmButton = { TextButton(onClick = { onRemove(request.id); removing = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancel") } })
    }
}
