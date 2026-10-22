package ch.argarden.app.features.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import ch.argarden.app.ui.*

@Composable fun AuthScreen(state: SessionState, register: Boolean, onSwitch: () -> Unit,
    onLogin: (String, String) -> Unit, onRegister: (String, String, String, String) -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    // Passwords deliberately stay out of saved instance state.
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    GardenPage(if (register) "Plant your first connection" else "Welcome back") {
        Text(if (register) "Create your AR Garden account." else "Sign in to your garden community.")
        if (register) OutlinedTextField(username, { username = it }, label = { Text("Username") },
            supportingText = { Text("3–24 letters, numbers or underscores") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        if (register) OutlinedTextField(confirm, { confirm = it }, label = { Text("Confirm password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        Status(state.busy, state.message)
        Action(if (register) "Register" else "Login", !state.busy) {
            if (register) onRegister(username, email, password, confirm) else onLogin(email, password)
        }
        TextButton(onClick = onSwitch, enabled = !state.busy) { Text(if (register) "Already have an account? Login" else "Create an account") }
    }
}
