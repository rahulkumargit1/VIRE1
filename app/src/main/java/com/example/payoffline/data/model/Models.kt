package com.example.payoffline.data.model

import java.util.UUID

data class SimInfo(
    val slotIndex: Int,
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String,
    val number: String? = null
)

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val type: TransactionType,
    val recipient: String = "",
    val amount: Double = 0.0,
    val simName: String = "",
    val status: TransactionStatus,
    val response: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class TransactionType {
    SEND_MONEY,
    CHECK_BALANCE,
    MINI_STATEMENT,
    LINK_BANK
}

enum class TransactionStatus {
    SUCCESS,
    FAILED,
    PENDING
}

data class UssdResult(
    val success: Boolean,
    val response: String,
    val errorType: UssdErrorType = UssdErrorType.NONE
)

enum class UssdErrorType {
    NONE,
    PERMISSION_DENIED,
    NO_SIM,
    NETWORK_FAILURE,
    CARRIER_NOT_SUPPORTED,
    TIMEOUT,
    UNKNOWN
}

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val message: String) : UiState()
    data class Error(val message: String, val errorType: UssdErrorType = UssdErrorType.UNKNOWN) : UiState()
}

data class AppSettings(
    val biometricEnabled: Boolean = false,
    val darkMode: Boolean = false,
    val hapticEnabled: Boolean = true,
    val defaultSimSlot: Int = -1,
    val savedRecipients: List<String> = emptyList()
)
