package app.shoutsend.feature.onboarding

import app.shoutsend.core.audio.permissions.MicPermissionState
import app.shoutsend.core.audio.permissions.PermissionRepository
import app.shoutsend.core.data.settings.FakeSettingsRepository
import app.shoutsend.core.data.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class TestPermissionRepository(
        initialState: MicPermissionState = MicPermissionState.CanAsk
    ) : PermissionRepository {
        val stateFlow = MutableStateFlow(initialState)
        override val micState: StateFlow<MicPermissionState> = stateFlow.asStateFlow()
        var refreshCallCount = 0

        override suspend fun refresh() {
            refreshCallCount++
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsPageZeroAndNotFinished() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val permissionRepo = TestPermissionRepository()
        val viewModel = OnboardingViewModel(settingsRepo, permissionRepo)

        testScheduler.advanceUntilIdle()

        assertEquals(0, viewModel.currentPage.value)
        val uiState = viewModel.uiState.value
        assertEquals(0, uiState.currentPage)
        assertEquals(4, uiState.totalPages)
        assertFalse(uiState.isOnboardingFinished)
    }

    @Test
    fun pageNavigationNextAndPrevious() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val viewModel = OnboardingViewModel(settingsRepo)

        viewModel.nextPage()
        assertEquals(1, viewModel.currentPage.value)

        viewModel.nextPage()
        assertEquals(2, viewModel.currentPage.value)

        viewModel.nextPage()
        assertEquals(3, viewModel.currentPage.value)

        // Cannot exceed page 3
        viewModel.nextPage()
        assertEquals(3, viewModel.currentPage.value)

        viewModel.previousPage()
        assertEquals(2, viewModel.currentPage.value)

        viewModel.previousPage()
        viewModel.previousPage()
        assertEquals(0, viewModel.currentPage.value)

        // Cannot go below page 0
        viewModel.previousPage()
        assertEquals(0, viewModel.currentPage.value)
    }

    @Test
    fun skipJumpsDirectlyToPermissionPage() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val viewModel = OnboardingViewModel(settingsRepo)

        assertEquals(0, viewModel.currentPage.value)
        viewModel.skipToPermission()
        assertEquals(3, viewModel.currentPage.value)
    }

    @Test
    fun completeOnboardingUpdatesSettingsAndTriggersCallback() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val viewModel = OnboardingViewModel(settingsRepo)

        var callbackCalled = false
        viewModel.completeOnboarding {
            callbackCalled = true
        }

        testScheduler.advanceUntilIdle()

        assertTrue(callbackCalled)
        assertTrue(settingsRepo.settings.first().onboardingCompleted)
    }

    @Test
    fun permissionRequestUpdatesSettingsAndRefreshes() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val permissionRepo = TestPermissionRepository()
        val viewModel = OnboardingViewModel(settingsRepo, permissionRepo)

        viewModel.onPermissionRequested()
        testScheduler.advanceUntilIdle()

        assertTrue(settingsRepo.settings.first().micPermissionRequested)
        assertEquals(1, permissionRepo.refreshCallCount)
    }

    @Test
    fun permissionGrantedResultUpdatesState() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val permissionRepo = TestPermissionRepository()
        val viewModel = OnboardingViewModel(settingsRepo, permissionRepo)

        viewModel.onPermissionResult(isGranted = true)
        testScheduler.advanceUntilIdle()

        assertTrue(settingsRepo.settings.first().micPermissionRequested)
        assertEquals(MicPermissionState.Granted, viewModel.uiState.value.micPermissionState)
    }

    @Test
    fun permissionDeniedResultUpdatesState() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val permissionRepo = TestPermissionRepository()
        val viewModel = OnboardingViewModel(settingsRepo, permissionRepo)

        viewModel.onPermissionResult(isGranted = false)
        testScheduler.advanceUntilIdle()

        assertTrue(settingsRepo.settings.first().micPermissionRequested)
        assertEquals(MicPermissionState.PermanentlyDenied, viewModel.uiState.value.micPermissionState)
    }
}
