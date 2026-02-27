package com.example.payoffline.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.payoffline.data.model.*
import com.example.payoffline.data.repository.HistoryRepository
import com.example.payoffline.data.repository.SettingsRepository
import com.example.payoffline.data.repository.UssdRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UssdViewModel(application: Application) : AndroidViewModel(application) {

    val ussdRepo    = UssdRepository(application)
    val historyRepo = HistoryRepository(application)
    val settingsRepo = SettingsRepository(application)

    // SIM
    private val _sims = MutableStateFlow<List<SimInfo>>(emptyList())
    val sims: StateFlow<List<SimInfo>> = _sims.asStateFlow()

    private val _selectedSim = MutableStateFlow<SimInfo?>(null)
    val selectedSim: StateFlow<SimInfo?> = _selectedSim.asStateFlow()

    // Pay form
    private val _recipient = MutableStateFlow("")
    val recipient: StateFlow<String> = _recipient.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _pin = MutableStateFlow("")
    val pin: StateFlow<String> = _pin.asStateFlow()

    // UI state
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _balanceUiState = MutableStateFlow<UiState>(UiState.Idle)
    val balanceUiState: StateFlow<UiState> = _balanceUiState.asStateFlow()

    // Permission error
    private val _permissionError = MutableStateFlow(false)
    val permissionError: StateFlow<Boolean> = _permissionError.asStateFlow()

    val transactions = historyRepo.transactions
    val settings     = settingsRepo.settings

    // Dialogs
    private val _showSimSelector = MutableStateFlow(false)
    val showSimSelector: StateFlow<Boolean> = _showSimSelector.asStateFlow()

    init {
        loadSims()
    }

    fun loadSims() {
        val list = ussdRepo.loadSims()
        _sims.value = list
        if (_selectedSim.value == null && list.isNotEmpty()) {
            // Auto-select based on saved preference
            val savedSlot = settingsRepo.settings.value.defaultSimSlot
            val preferred = list.firstOrNull { it.slotIndex == savedSlot } ?: list.first()
            _selectedSim.value = preferred
        }
    }

    fun selectSim(sim: SimInfo) {
        _selectedSim.value = sim
        _showSimSelector.value = false
    }

    fun onRecipientChange(v: String) { _recipient.value = v }
    fun onAmountChange(v: String)    { _amount.value = v }
    fun onPinChange(v: String)       { _pin.value = v }
    fun setShowSimSelector(show: Boolean) { _showSimSelector.value = show }

    fun clearForm() {
        _recipient.value = ""
        _amount.value    = ""
        _pin.value       = ""
        _uiState.value   = UiState.Idle
    }

    fun clearResponse() { _uiState.value = UiState.Idle }
    fun clearBalanceResponse() { _balanceUiState.value = UiState.Idle }

    // ─── Send Money ───────────────────────────────────────────────────────────
    fun sendMoney() {
        val sim = _selectedSim.value ?: run {
            _uiState.value = UiState.Error("No SIM selected. Please select a SIM card.", UssdErrorType.NO_SIM)
            return
        }
        if (_recipient.value.isBlank()) {
            _uiState.value = UiState.Error("Please enter a recipient mobile number or UPI ID.")
            return
        }
        if (_amount.value.isBlank()) {
            _uiState.value = UiState.Error("Please enter an amount.")
            return
        }
        if (_pin.value.length < 4) {
            _uiState.value = UiState.Error("Please enter your UPI PIN (4–6 digits).")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = ussdRepo.sendMoney(sim, _recipient.value.trim(), _amount.value.trim(), _pin.value.trim())
            _uiState.value = if (result.success)
                UiState.Success(result.response)
            else
                UiState.Error(result.response, result.errorType)

            historyRepo.addTransaction(Transaction(
                type      = TransactionType.SEND_MONEY,
                recipient = _recipient.value.trim(),
                amount    = _amount.value.toDoubleOrNull() ?: 0.0,
                simName   = sim.displayName,
                status    = if (result.success) TransactionStatus.SUCCESS else TransactionStatus.FAILED,
                response  = result.response
            ))

            if (result.success) {
                settingsRepo.addRecipient(_recipient.value.trim())
            }
        }
    }

    // ─── Check Balance ────────────────────────────────────────────────────────
    fun checkBalance(pin: String) {
        val sim = _selectedSim.value ?: run {
            _balanceUiState.value = UiState.Error("No SIM selected.", UssdErrorType.NO_SIM)
            return
        }
        if (pin.length < 4) {
            _balanceUiState.value = UiState.Error("Please enter your UPI PIN.")
            return
        }
        viewModelScope.launch {
            _balanceUiState.value = UiState.Loading
            val result = ussdRepo.checkBalance(sim, pin)
            _balanceUiState.value = if (result.success)
                UiState.Success(result.response)
            else
                UiState.Error(result.response, result.errorType)

            historyRepo.addTransaction(Transaction(
                type    = TransactionType.CHECK_BALANCE,
                simName = sim.displayName,
                status  = if (result.success) TransactionStatus.SUCCESS else TransactionStatus.FAILED,
                response = result.response
            ))
        }
    }

    // ─── Mini Statement ───────────────────────────────────────────────────────
    fun miniStatement(pin: String) {
        val sim = _selectedSim.value ?: run {
            _balanceUiState.value = UiState.Error("No SIM selected.", UssdErrorType.NO_SIM)
            return
        }
        viewModelScope.launch {
            _balanceUiState.value = UiState.Loading
            val result = ussdRepo.miniStatement(sim, pin)
            _balanceUiState.value = if (result.success)
                UiState.Success(result.response)
            else
                UiState.Error(result.response, result.errorType)

            historyRepo.addTransaction(Transaction(
                type    = TransactionType.MINI_STATEMENT,
                simName = sim.displayName,
                status  = if (result.success) TransactionStatus.SUCCESS else TransactionStatus.FAILED,
                response = result.response
            ))
        }
    }

    // ─── Link Bank ────────────────────────────────────────────────────────────
    fun linkBankAccount(pin: String) {
        val sim = _selectedSim.value ?: run {
            _balanceUiState.value = UiState.Error("No SIM selected.", UssdErrorType.NO_SIM)
            return
        }
        viewModelScope.launch {
            _balanceUiState.value = UiState.Loading
            val result = ussdRepo.linkBankAccount(sim, pin)
            _balanceUiState.value = if (result.success)
                UiState.Success(result.response)
            else
                UiState.Error(result.response, result.errorType)
        }
    }

    // ─── History ──────────────────────────────────────────────────────────────
    fun clearHistory() {
        viewModelScope.launch { historyRepo.clearHistory() }
    }

    fun exportHistory() {
        viewModelScope.launch {
            val uri = historyRepo.exportToCsv()
            uri?.let { historyRepo.shareHistory(it) }
        }
    }

    // ─── Settings ─────────────────────────────────────────────────────────────
    fun updateSettings(s: AppSettings) {
        viewModelScope.launch { settingsRepo.updateSettings(s) }
    }

    // ─── Dialer fallback ──────────────────────────────────────────────────────
    fun openDialerFallback(type: String) {
        val sim = _selectedSim.value ?: return
        val code = when (type) {
            "send"    -> "${UssdRepository.USSD_SEND_MONEY}${_recipient.value}*${_amount.value}*${_pin.value}#"
            "balance" -> UssdRepository.USSD_CHECK_BAL
            "mini"    -> UssdRepository.USSD_MINI_STMT
            else      -> UssdRepository.USSD_BASE
        }
        ussdRepo.openDialerWithUssd(code)
    }

    fun setPermissionError(value: Boolean) { _permissionError.value = value }
}
