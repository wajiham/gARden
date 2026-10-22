package ch.argarden.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.argarden.app.data.remote.*
import ch.argarden.app.features.auth.*
import ch.argarden.app.features.profile.ProfileScreen
import ch.argarden.app.features.social.SocialViewModel
import ch.argarden.app.navigation.*
import ch.argarden.app.ui.*
import ch.argarden.app.ui.theme.GardenTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val configured = FirebaseApp.initializeApp(this) != null
        setContent {
            GardenTheme {
                if (!configured) GardenPage("Connect your garden") {
                    Text("Firebase setup is required.")
                    Text("Add your project's google-services.json to the app folder, enable Email/Password Authentication, deploy the Firestore rules, then rebuild.")
                    Text("See docs/FIREBASE_SETUP.md in the ARGarden project.")
                } else {
                    val profiles = remember { FirebaseProfileRepository(FirebaseFirestore.getInstance()) }
                    val vm: SessionViewModel = viewModel(factory = factory {
                        SessionViewModel(FirebaseAuthRepository(FirebaseAuth.getInstance()), profiles)
                    })
                    val state by vm.state.collectAsStateWithLifecycle()
                    when {
                        state.initializing || state.profileLoading -> GardenPage("Growing your space…") { Status(true, null) }
                        state.account == null -> AuthNavigation(state, vm)
                        state.profile == null -> ProfileScreen(state, null, vm::saveUsername, vm::logout, onRetry = vm::retryProfile)
                        else -> key(state.account!!.uid) {
                            // Clear account-scoped listeners and state when logging out/switching users.
                            val owner = remember { object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() } }
                            DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
                            val socialVm: SocialViewModel = viewModel(viewModelStoreOwner = owner, factory = factory {
                                SocialViewModel(state.account!!.uid, FirebaseSocialRepository(FirebaseFirestore.getInstance()), profiles)
                            })
                            val social by socialVm.state.collectAsStateWithLifecycle()
                            GardenNavigation(state, social, vm::saveUsername, vm::logout, vm::retryProfile, socialVm)
                        }
                    }
                }
            }
        }
    }
}
private fun <T : ViewModel> factory(create: () -> T) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <V : ViewModel> create(modelClass: Class<V>): V = create() as V
}
