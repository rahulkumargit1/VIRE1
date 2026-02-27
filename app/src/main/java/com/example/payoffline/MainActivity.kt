package com.example.payoffline

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.payoffline.ui.screens.*
import com.example.payoffline.ui.theme.PayOfflineTheme
import com.example.payoffline.viewmodel.UssdViewModel

sealed class NavTab(val route: String, val label: String, val icon: ImageVector, val iconSelected: ImageVector) {
    object Pay     : NavTab("pay",     "Pay",      Icons.Outlined.Send,           Icons.Filled.Send)
    object Balance : NavTab("balance", "Balance",  Icons.Outlined.AccountBalance, Icons.Filled.AccountBalance)
    object History : NavTab("history", "History",  Icons.Outlined.ReceiptLong,    Icons.Filled.ReceiptLong)
    object Settings: NavTab("settings","Settings", Icons.Outlined.Settings,       Icons.Filled.Settings)
}

val allTabs = listOf(NavTab.Pay, NavTab.Balance, NavTab.History, NavTab.Settings)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: UssdViewModel = viewModel()
            val settings by vm.settings.collectAsState()

            PayOfflineTheme(darkTheme = settings.darkMode) {
                AppContent(vm = vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(vm: UssdViewModel) {
    val context  = LocalContext.current
    val settings by vm.settings.collectAsState()

    var currentTab       by remember { mutableStateOf<NavTab>(NavTab.Pay) }
    var hasCallPermission by remember { mutableStateOf(false) }
    var isAuthenticated  by remember { mutableStateOf(!settings.biometricEnabled) }

    // Check initial permission state
    LaunchedEffect(Unit) {
        hasCallPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        vm.loadSims()
    }

    // Re-check auth when biometric setting changes
    LaunchedEffect(settings.biometricEnabled) {
        if (!settings.biometricEnabled) isAuthenticated = true
    }

    // Permission launcher
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasCallPermission = results[Manifest.permission.CALL_PHONE] == true
        if (results[Manifest.permission.READ_PHONE_STATE] == true ||
            results[Manifest.permission.READ_PHONE_NUMBERS] == true) {
            vm.loadSims()
        }
    }

    // Request permissions on start
    LaunchedEffect(Unit) {
        permLauncher.launch(arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS
        ))
    }

    // Biometric auth
    if (!isAuthenticated && settings.biometricEnabled) {
        LaunchedEffect(Unit) {
            val activity = context as? ComponentActivity ?: return@LaunchedEffect
            val bioManager = BiometricManager.from(context)
            if (bioManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS) {
                val executor = ContextCompat.getMainExecutor(context)
                val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        isAuthenticated = true
                    }
                })
                val info = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("PayOffline")
                    .setSubtitle("Authenticate to continue")
                    .setNegativeButtonText("Cancel")
                    .build()
                prompt.authenticate(info)
            } else {
                // Biometric not available, skip auth
                isAuthenticated = true
            }
        }
    }

    if (!isAuthenticated) {
        // Splash/lock screen while authenticating
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.CurrencyRupee, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("PayOffline", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Authenticating…", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(tonalElevation = 8.dp) {
                allTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick  = { currentTab = tab },
                        icon     = {
                            Icon(
                                if (currentTab == tab) tab.iconSelected else tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label    = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                },
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    NavTab.Pay -> PayScreen(
                        vm = vm,
                        onRequestPermission = {
                            permLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
                        },
                        hasCallPermission = hasCallPermission
                    )
                    NavTab.Balance  -> BalanceScreen(vm)
                    NavTab.History  -> HistoryScreen(vm)
                    NavTab.Settings -> SettingsScreen(vm)
                }
            }
        }
    }
}
