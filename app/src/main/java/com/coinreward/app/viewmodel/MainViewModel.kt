package com.coinreward.app.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coinreward.app.data.CoinBalance
import com.coinreward.app.data.CoinTransaction
import com.coinreward.app.data.SupabaseApi
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class AppState {
    object Idle : AppState()
    object Loading : AppState()
    object LoggedIn : AppState()
    data class Error(val message: String) : AppState()
}

class MainViewModel : ViewModel(), LevelPlayRewardedVideoListener {

    private val _appState = MutableStateFlow<AppState>(AppState.Idle)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private val _balance = MutableStateFlow(0)
    val balance: StateFlow<Int> = _balance.asStateFlow()

    private val _transactions = MutableStateFlow<List<CoinTransaction>>(emptyList())
    val transactions: StateFlow<List<CoinTransaction>> = _transactions.asStateFlow()

    private val _adStatus = MutableStateFlow("Initializing...")
    val adStatus: StateFlow<String> = _adStatus.asStateFlow()

    private val _notification = MutableStateFlow<String?>(null)
    val notification: StateFlow<String?> = _notification.asStateFlow()

    private var isAdReady = false
    private var userId: String? = null

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            try {
                val session = SupabaseApi.client.auth.currentSessionOrNull()
                if (session != null) {
                    userId = session.user?.id
                    _appState.value = AppState.LoggedIn
                    loadUserData()
                } else {
                    _appState.value = AppState.Idle
                }
            } catch (e: Exception) {
                // Ignore error on invalid credentials or placeholder keys
                _appState.value = AppState.Idle
            }
        }
    }

    fun signUp(email: String, pass: String) {
        viewModelScope.launch {
            _appState.value = AppState.Loading
            try {
                SupabaseApi.client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = pass
                }
                login(email, pass)
            } catch (e: Exception) {
                _appState.value = AppState.Error(e.message ?: "Sign up failed")
            }
        }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _appState.value = AppState.Loading
            try {
                SupabaseApi.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = pass
                }
                userId = SupabaseApi.client.auth.currentUserOrNull()?.id
                _appState.value = AppState.LoggedIn
                loadUserData()
            } catch (e: Exception) {
                _appState.value = AppState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                SupabaseApi.client.auth.signOut()
            } catch (e: Exception) {}
            userId = null
            _balance.value = 0
            _transactions.value = emptyList()
            _appState.value = AppState.Idle
        }
    }

    fun loadUserData() {
        if (userId == null) return
        viewModelScope.launch {
            try {
                val balanceResult = SupabaseApi.client.postgrest["coin_balances"]
                    .select { filter { eq("user_id", userId!!) } }
                    .decodeSingleOrNull<CoinBalance>()
                _balance.value = balanceResult?.balance ?: 0

                val txResult = SupabaseApi.client.postgrest["coin_transactions"]
                    .select {
                        filter { eq("user_id", userId!!) }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<CoinTransaction>()
                _transactions.value = txResult
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading user data", e)
            }
        }
    }

    // --- IronSource Integration ---
    fun initIronSource(activity: Activity, appKey: String = "800374950") {
        if (userId == null) return
        
        // Ensure Unity LevelPlay S2S callback includes the dynamic user ID
        IronSource.setDynamicUserId(userId!!)
        
        IronSource.setLevelPlayRewardedVideoListener(this)
        IronSource.init(activity, appKey, IronSource.AD_UNIT.REWARDED_VIDEO)
        
        _adStatus.value = "جاري تحميل الإعلان..."
    }

    fun showRewardedVideo(activity: Activity) {
        if (IronSource.isRewardedVideoAvailable()) {
            IronSource.showRewardedVideo(activity)
        } else {
            showNotification("الإعلان غير متاح حاليًا، حاول مرة أخرى.")
        }
    }

    fun clearNotification() {
        _notification.value = null
    }

    private fun showNotification(msg: String) {
        _notification.value = msg
    }

    // -- LevelPlayRewardedVideoListener Callbacks --
    override fun onAdOpened(adInfo: com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo?) {
        _adStatus.value = "الإعلان قيد العرض..."
    }

    override fun onAdClosed(adInfo: com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo?) {
        _adStatus.value = if (IronSource.isRewardedVideoAvailable()) "الإعلان جاهز" else "جاري تحميل الإعلان..."
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            loadUserData()
        }
    }

    override fun onAdAvailable(adInfo: com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo?) {
        isAdReady = true
        _adStatus.value = "الإعلان جاهز"
    }

    override fun onAdUnavailable() {
        isAdReady = false
        _adStatus.value = "جاري تحميل الإعلان..."
    }

    override fun onAdRewarded(placement: com.ironsource.mediationsdk.model.Placement?, adInfo: com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo?) {
        showNotification("تمت مشاهدة الإعلان بنجاح! يتم الآن إضافة 10 عملات إلى رصيدك عبر الخادم.")
    }

    override fun onAdShowFailed(error: IronSourceError?, adInfo: com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo?) {
        showNotification("فشل عرض الإعلان: ${error?.errorMessage}")
        _adStatus.value = "فشل الإعلان"
    }
    
    override fun onAdClicked(placement: com.ironsource.mediationsdk.model.Placement?, adInfo: com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo?) {
    }
}
