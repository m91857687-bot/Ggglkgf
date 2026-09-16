package com.coinreward.app.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MainViewModelTest {

    @Test
    fun testInitialState() {
        // Just a basic test to satisfy the test requirement structure.
        // Full testing of Supabase and IronSource requires extensive mocking.
        val viewModel = MainViewModel()
        
        // Initial state should be Idle since keys are missing/mocked
        assertEquals(AppState.Idle, viewModel.appState.value)
        assertEquals(0, viewModel.balance.value)
        assertEquals("Initializing...", viewModel.adStatus.value)
    }
    
    @Test
    fun testAdClosedDoesNotAddCoinsDirectly() {
        val viewModel = MainViewModel()
        
        // Simulate ad closing early or finishing
        viewModel.onRewardedVideoAdClosed()
        
        // Balance should NOT increase directly from the Android app
        // It waits for the Supabase DB callback to update it on the next fetch
        assertEquals(0, viewModel.balance.value)
    }
}
