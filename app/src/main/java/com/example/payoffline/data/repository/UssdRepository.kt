package com.example.payoffline.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.payoffline.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * USSD Repository — handles all USSD calls with:
 * 1. Primary method: TelephonyManager.sendUssdRequest (requires CALL_PHONE)
 * 2. Fallback method: Intent ACTION_DIAL (no permission needed)
 * 3. Carrier compatibility detection
 * 4. Proper error classification
 */
class UssdRepository(private val context: Context) {

    companion object {
        // *99# USSD base codes (NPCI standard)
        const val USSD_BASE = "*99#"
        const val USSD_SEND_MONEY  = "*99*1*"    // append recipient*amount*pin#
        const val USSD_CHECK_BAL   = "*99*5#"    // check balance (with UPI PIN)
        const val USSD_MINI_STMT   = "*99*3#"    // mini statement (with UPI PIN)
        const val USSD_LINK_BANK   = "*99*2*"    // link bank account
        const val USSD_TIMEOUT_MS  = 30_000L
    }

    /** Check if CALL_PHONE permission is granted */
    fun hasCallPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                PackageManager.PERMISSION_GRANTED

    /** Check if READ_PHONE_STATE is granted */
    fun hasPhoneStatePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
                PackageManager.PERMISSION_GRANTED

    /**
     * Load all available SIM cards
     */
    @SuppressLint("MissingPermission")
    fun loadSims(): List<SimInfo> {
        if (!hasPhoneStatePermission()) return emptyList()
        return try {
            val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                    as? SubscriptionManager ?: return emptyList()
            val subs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                subManager.activeSubscriptionInfoList
            else
                @Suppress("DEPRECATION") subManager.activeSubscriptionInfoList

            subs?.mapIndexed { index, info ->
                SimInfo(
                    slotIndex      = info.simSlotIndex,
                    subscriptionId = info.subscriptionId,
                    displayName    = info.displayName?.toString() ?: "SIM ${index + 1}",
                    carrierName    = info.carrierName?.toString() ?: "Unknown Carrier",
                    number         = info.number?.takeIf { it.isNotBlank() }
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Send money via USSD *99*1*recipient*amount*pin#
     */
    suspend fun sendMoney(
        sim: SimInfo,
        recipient: String,
        amount: String,
        pin: String
    ): UssdResult = withContext(Dispatchers.IO) {
        val code = "$USSD_SEND_MONEY${recipient}*${amount}*${pin}#"
        runUssd(sim, code)
    }

    /**
     * Check balance via USSD *99*5#
     */
    suspend fun checkBalance(sim: SimInfo, pin: String): UssdResult =
        withContext(Dispatchers.IO) {
            val code = "*99*5*${pin}#"
            runUssd(sim, code)
        }

    /**
     * Mini statement via USSD *99*3#
     */
    suspend fun miniStatement(sim: SimInfo, pin: String): UssdResult =
        withContext(Dispatchers.IO) {
            val code = "*99*3*${pin}#"
            runUssd(sim, code)
        }

    /**
     * Link bank account via USSD *99*2*pin#
     */
    suspend fun linkBankAccount(sim: SimInfo, pin: String): UssdResult =
        withContext(Dispatchers.IO) {
            val code = "$USSD_LINK_BANK${pin}#"
            runUssd(sim, code)
        }

    /**
     * Core USSD runner:
     * - Tries TelephonyManager.sendUssdRequest first (programmatic)
     * - If permission denied, launches fallback dialer intent
     * - Properly classifies all error types
     */
    @SuppressLint("MissingPermission")
    private suspend fun runUssd(sim: SimInfo, code: String): UssdResult {
        // --- Method 1: Programmatic USSD (requires CALL_PHONE) ---
        if (hasCallPermission()) {
            return runUssdProgrammatic(sim, code)
        }

        // --- Method 2: Dialer Intent Fallback (no permission needed) ---
        return runUssdViaDialer(code)
    }

    @SuppressLint("MissingPermission")
    private suspend fun runUssdProgrammatic(sim: SimInfo, code: String): UssdResult =
        suspendCancellableCoroutine { cont ->
            try {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE)
                        as? TelephonyManager

                if (telephonyManager == null) {
                    cont.resume(UssdResult(false, "Telephony service unavailable.", UssdErrorType.UNKNOWN))
                    return@suspendCancellableCoroutine
                }

                // Use subscription-specific TelephonyManager for dual SIM
                val simTelephonyManager = telephonyManager.createForSubscriptionId(sim.subscriptionId)

                val callback = object : TelephonyManager.UssdResponseCallback() {
                    override fun onReceiveUssdResponse(
                        telephonyManager: TelephonyManager,
                        request: String,
                        response: CharSequence
                    ) {
                        if (cont.isActive) {
                            cont.resume(UssdResult(true, response.toString()))
                        }
                    }

                    override fun onReceiveUssdResponseFailed(
                        telephonyManager: TelephonyManager,
                        request: String,
                        failureCode: Int
                    ) {
                        if (cont.isActive) {
                            val (msg, type) = classifyUssdFailure(failureCode)
                            cont.resume(UssdResult(false, msg, type))
                        }
                    }
                }

                simTelephonyManager.sendUssdRequest(
                    code,
                    callback,
                    Handler(Looper.getMainLooper())
                )

                // Timeout guard
                Handler(Looper.getMainLooper()).postDelayed({
                    if (cont.isActive) {
                        cont.resume(UssdResult(false, "Request timed out. Please try again.", UssdErrorType.TIMEOUT))
                    }
                }, USSD_TIMEOUT_MS)

            } catch (se: SecurityException) {
                cont.resume(UssdResult(false, "Permission denied. Please grant CALL_PHONE permission.", UssdErrorType.PERMISSION_DENIED))
            } catch (e: Exception) {
                cont.resume(UssdResult(false, "Unexpected error: ${e.localizedMessage}", UssdErrorType.UNKNOWN))
            }
        }

    /**
     * Fallback: Open dialer with USSD code pre-filled.
     * User just needs to tap the call button.
     * This requires NO permissions.
     */
    private fun runUssdViaDialer(code: String): UssdResult {
        return try {
            val encoded = Uri.encode(code)
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$encoded")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            UssdResult(
                success = true,
                response = "Dialer opened with USSD code. Please tap the Call button to proceed.",
                errorType = UssdErrorType.NONE
            )
        } catch (e: Exception) {
            UssdResult(
                success = false,
                response = "Could not open dialer: ${e.localizedMessage}",
                errorType = UssdErrorType.UNKNOWN
            )
        }
    }

    /**
     * Classify Android USSD failure codes into user-friendly messages
     */
    private fun classifyUssdFailure(code: Int): Pair<String, UssdErrorType> {
        return when (code) {
            TelephonyManager.USSD_RETURN_FAILURE ->
                Pair(
                    "Network returned failure. Your carrier may not support *99# USSD. Try using the Dialer method.",
                    UssdErrorType.CARRIER_NOT_SUPPORTED
                )
            TelephonyManager.USSD_ERROR_SERVICE_UNAVAIL ->
                Pair(
                    "USSD service is unavailable. Check your network signal and try again.",
                    UssdErrorType.NETWORK_FAILURE
                )
            else ->
                Pair(
                    "Request failed (code $code). Please check your signal and try again.",
                    UssdErrorType.UNKNOWN
                )
        }
    }

    /**
     * Launch dialer explicitly for a given USSD code.
     * Used as a manual alternative from the UI.
     */
    fun openDialerWithUssd(code: String) {
        try {
            val encoded = Uri.encode(code)
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$encoded")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
    }
}
