package ch.argarden.app

import ch.argarden.app.data.model.*
import ch.argarden.app.data.repository.*
import ch.argarden.app.features.auth.SessionViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    private class FakeAuth(initial: Account? = null) : AuthRepository {
        override val accounts = MutableStateFlow(initial)
        var loginCalls = 0
        override suspend fun register(email: String, password: String) { accounts.value = Account("new", email) }
        override suspend fun login(email: String, password: String) { loginCalls++; accounts.value = Account("alice", email) }
        override fun logout() { accounts.value = null }
    }
    private class FakeProfiles : ProfileRepository {
        val profile = MutableStateFlow<User?>(null)
        var fail = false
        override fun observe(uid: String) = profile
        override suspend fun saveUsername(uid: String, username: String) {
            if (fail) error("Profile save failed. Retry.")
            profile.value = User(uid, username, Validation.normalize(username))
        }
        override suspend fun search(normalizedUsername: String) = emptyList<User>()
    }
    @Test fun restoredAccountLoadsProfileAndLogoutClearsIt() = runTest(dispatcher) {
        val auth = FakeAuth(Account("alice", "a@example.com"))
        val profiles = FakeProfiles().apply { profile.value = User("alice", "Alice", "alice") }
        val vm = SessionViewModel(auth, profiles)
        runCurrent()
        assertFalse(vm.state.value.initializing)
        assertEquals("Alice", vm.state.value.profile?.username)
        vm.logout(); runCurrent()
        assertNull(vm.state.value.account)
        assertNull(vm.state.value.profile)
        vm.viewModelScopeForTestCleanup()
    }
    @Test fun invalidLoginNeverCallsBackendAndValidLoginUpdatesSession() = runTest(dispatcher) {
        val auth = FakeAuth()
        val vm = SessionViewModel(auth, FakeProfiles())
        runCurrent()
        vm.login("invalid", "x"); runCurrent()
        assertEquals(0, auth.loginCalls)
        assertNotNull(vm.state.value.message)
        vm.login("a@example.com", "password"); runCurrent()
        assertEquals(1, auth.loginCalls)
        assertEquals("alice", vm.state.value.account?.uid)
        vm.viewModelScopeForTestCleanup()
    }
    @Test fun partialRegistrationCanRecoverWithoutCreatingAnotherAccount() = runTest(dispatcher) {
        val profiles = FakeProfiles().apply { fail = true }
        val vm = SessionViewModel(FakeAuth(), profiles)
        runCurrent()
        vm.register("Alice", "a@example.com", "password", "password"); runCurrent()
        assertNotNull(vm.state.value.account)
        assertNull(vm.state.value.profile)
        assertNotNull(vm.state.value.message)
        profiles.fail = false
        vm.saveUsername("Alice"); runCurrent()
        assertEquals("Alice", vm.state.value.profile?.username)
        vm.viewModelScopeForTestCleanup()
    }
}

private fun SessionViewModel.viewModelScopeForTestCleanup() {
    // ViewModelStore.clear invokes the same cleanup as the Activity lifecycle.
    androidx.lifecycle.ViewModelStore().also { it.put("test", this); it.clear() }
}
