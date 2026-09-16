package com.coinreward.app.data

import kotlinx.serialization.Serializable

@Serializable
data class CoinBalance(
    val user_id: String,
    val balance: Int
)

@Serializable
data class CoinTransaction(
    val id: String,
    val user_id: String,
    val amount: Int,
    val type: String,
    val event_id: String,
    val created_at: String
)
