package com.coinreward.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.coinreward.app.ui.theme.CoinRewardTheme
import com.coinreward.app.viewmodel.AppState
import com.coinreward.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CoinRewardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val appState by viewModel.appState.collectAsState()
                    
                    when (appState) {
                        is AppState.LoggedIn -> MainScreen(viewModel)
                        else -> AuthScreen(viewModel)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // IronSource lifecycle requirement
        com.ironsource.mediationsdk.IronSource.onResume(this)
        
        // Refresh balance when returning to app
        viewModel.loadUserData()
    }

    override fun onPause() {
        super.onPause()
        com.ironsource.mediationsdk.IronSource.onPause(this)
    }
}
