package com.fiepi.media.app.ui.screen.browser.viewmodel

import android.app.Application
import androidx.paging.PagingData
import app.cash.turbine.test
import com.fiepi.media.app.service.FileOperationManager
import com.fiepi.media.domain.model.history.PlaybackHistory
import com.fiepi.media.domain.model.media.FileOperationStatus
import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.preferences.MediaField
import com.fiepi.media.domain.model.preferences.MediaOptions
import com.fiepi.media.domain.model.source.MediaSource
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.usecases.BrowserUseCases
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserViewModelTest {

    private val application = mockk<Application>(relaxed = true)
    private val useCases = mockk<BrowserUseCases>(relaxed = true)
    private val fileOperationManager = mockk<FileOperationManager>(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    private val sourcesFlow = MutableStateFlow<List<MediaSource>>(emptyList())
    private val selectedSourceIdFlow = MutableStateFlow<String?>(MediaSource.INTERNAL_STORAGE_ID)
    private val interceptBackFlow = MutableStateFlow(true)
    private val sortOptionsFlow = MutableStateFlow(MediaOptions())
    private val displayFieldsFlow = MutableStateFlow<List<MediaField>>(MediaField.entries)
    private val fileOpStatusFlow = MutableStateFlow<FileOperationStatus>(FileOperationStatus.Idle)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { useCases.getSources() } returns sourcesFlow
        every { useCases.getSelectedSourceId() } returns selectedSourceIdFlow
        every { useCases.getInterceptBackNavigation() } returns interceptBackFlow
        every { useCases.getSortOptions(any()) } returns sortOptionsFlow
        every { useCases.getDisplayFields(any()) } returns displayFieldsFlow
        every { useCases.hasCachedMedia(any(), any()) } returns false
        every { fileOperationManager.status } returns fileOpStatusFlow

        every { application.getString(any()) } returns "Internal Storage"
        coEvery { useCases.getMediaFiles(any(), any(), any(), any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() = runTest {
        val viewModel = BrowserViewModel(application, useCases, fileOperationManager)
        runCurrent()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(MediaSource.INTERNAL_STORAGE_ID, state.source.current.id)
        }
    }

    @Test
    fun testNavigateToFolder() = runTest {
        val viewModel = BrowserViewModel(application, useCases, fileOperationManager)
        runCurrent()

        viewModel.state.test {
            skipItems(1)

            // Grant permission first to allow navigation/loading to proceed
            viewModel.onIntent(BrowserIntent.Storage.OnPermissionResult(true))
            runCurrent()

            val targetPath = "/test/folder"
            viewModel.onIntent(BrowserIntent.Storage.NavigateToFolder(targetPath, SourceType.Ftp))

            advanceTimeBy(200.milliseconds)
            runCurrent()

            val state = expectMostRecentItem()
            assertEquals(targetPath, state.mediaNavigationState.currentPath)
        }
    }

    @Test
    fun testSearchQueryChange() = runTest {
        val matrix =
            MediaFile.Video(path = "p1", name = "Matrix", sourceType = SourceType.Local, id = "1")

        // Mock search to return Matrix when queried
        every { useCases.searchMediaFiles("Matrix", any(), any()) } returns flowOf(matrix)

        val viewModel = BrowserViewModel(application, useCases, fileOperationManager)
        runCurrent()

        viewModel.state.test {
            // Grant permission
            viewModel.onIntent(BrowserIntent.Storage.OnPermissionResult(true))
            runCurrent()

            // Perform search
            viewModel.onIntent(BrowserIntent.Config.SearchQueryChange("Matrix"))

            // Search has debounce(300.milliseconds)
            advanceTimeBy(400.milliseconds)
            runCurrent()

            val state = expectMostRecentItem()
            assertEquals(1, state.search.results.size, "Should find 1 video matching 'Matrix'")
            assertEquals("Matrix", state.search.results[0].name)
        }
    }

    @Test
    fun testSwitchSource() = runTest {
        val viewModel = BrowserViewModel(application, useCases, fileOperationManager)
        runCurrent()

        viewModel.state.test {
            val newSource = MediaSource.Local("new_id", "New Source", "/new/root")
            sourcesFlow.value = listOf(newSource)
            selectedSourceIdFlow.value = "new_id"

            runCurrent()
            advanceTimeBy(200.milliseconds)
            runCurrent()

            val state = expectMostRecentItem()
            assertEquals("new_id", state.source.current.id)
            assertEquals("/new/root", state.mediaNavigationState.currentPath)
        }
    }

    @Test
    fun testSwitchToHistory() = runTest {
        val viewModel = BrowserViewModel(application, useCases, fileOperationManager)
        runCurrent()

        viewModel.state.test {
            skipItems(1)

            viewModel.onIntent(BrowserIntent.History.SwitchToHistory)
            runCurrent()

            val state = expectMostRecentItem()
            assertEquals(BrowserMode.History, state.mode)
        }
    }

    @Test
    fun testHistorySearch() = runTest {
        val historyItem = PlaybackHistory(
            sourceId = MediaSource.INTERNAL_STORAGE_ID,
            path = "/storage/emulated/0/Movies/Inception.mp4",
            title = "Inception"
        )
        every { useCases.getHistoryPaging(any(), any()) } returns flowOf(
            PagingData.from(
                listOf(
                    historyItem
                )
            )
        )

        val viewModel = BrowserViewModel(application, useCases, fileOperationManager)
        runCurrent()

        viewModel.historyPagingFlow.test {
            viewModel.onIntent(BrowserIntent.History.SwitchToHistory)
            runCurrent()

            viewModel.onIntent(BrowserIntent.Config.SearchQueryChange("Inception"))
            advanceTimeBy(400.milliseconds)
            runCurrent()

            expectMostRecentItem()
        }
    }
}
