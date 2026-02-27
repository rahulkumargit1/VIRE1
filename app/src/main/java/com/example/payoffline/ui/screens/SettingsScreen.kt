package com.example.payoffline.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.payoffline.data.model.AppSettings
import com.example.payoffline.ui.theme.Indigo600
import com.example.payoffline.viewmodel.UssdViewModel

@Composable
fun SettingsScreen(vm: UssdViewModel) {
    val settings by vm.settings.collectAsState()
    val scrollState = rememberScrollState()
    var showAbout by remember { mutableStateOf(false) }
    var showClearRecipients by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Header
        Surface(tonalElevation = 3.dp) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Appearance
            SettingsSection("Appearance") {
                SettingsToggleRow(
                    icon    = Icons.Filled.DarkMode,
                    title   = "Dark Mode",
                    subtitle = "Use dark theme throughout the app",
                    checked = settings.darkMode,
                    onCheckedChange = { vm.updateSettings(settings.copy(darkMode = it)) }
                )
                SettingsDivider()
                SettingsToggleRow(
                    icon    = Icons.Filled.Vibration,
                    title   = "Haptic Feedback",
                    subtitle = "Vibrate on actions and confirmations",
                    checked = settings.hapticEnabled,
                    onCheckedChange = { vm.updateSettings(settings.copy(hapticEnabled = it)) }
                )
            }

            Spacer(Modifier.height(8.dp))

            // Security
            SettingsSection("Security") {
                SettingsToggleRow(
                    icon    = Icons.Filled.Fingerprint,
                    title   = "App Lock",
                    subtitle = "Require biometric to open app",
                    checked = settings.biometricEnabled,
                    onCheckedChange = { vm.updateSettings(settings.copy(biometricEnabled = it)) }
                )
            }

            Spacer(Modifier.height(8.dp))

            // Data
            SettingsSection("Data") {
                SettingsActionRow(
                    icon     = Icons.Filled.FileDownload,
                    title    = "Export History",
                    subtitle = "Save transaction history as CSV",
                    onClick  = { vm.exportHistory() }
                )
                SettingsDivider()
                SettingsActionRow(
                    icon     = Icons.Filled.PersonRemove,
                    title    = "Clear Saved Recipients",
                    subtitle = "${settings.savedRecipients.size} saved recipients",
                    onClick  = { showClearRecipients = true }
                )
                SettingsDivider()
                SettingsActionRow(
                    icon     = Icons.Filled.DeleteOutline,
                    title    = "Clear Transaction History",
                    subtitle = "Permanently remove all history",
                    onClick  = { vm.clearHistory() },
                    dangerColor = true
                )
            }

            Spacer(Modifier.height(8.dp))

            // About
            SettingsSection("About") {
                SettingsActionRow(
                    icon    = Icons.Filled.Info,
                    title   = "About PayOffline",
                    subtitle = "Version 2.0.0",
                    onClick = { showAbout = true }
                )
                SettingsDivider()
                SettingsActionRow(
                    icon    = Icons.Filled.BugReport,
                    title   = "USSD Diagnostics",
                    subtitle = "Test your carrier compatibility",
                    onClick = { vm.openDialerFallback("base") }
                )
            }
        }

        Spacer(Modifier.height(80.dp))
    }

    // About dialog
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("About PayOffline") },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // App icon representation
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(72.dp),
                                shape    = RoundedCornerShape(18.dp),
                                color    = Indigo600
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.CurrencyRupee, null, tint = Color.White,
                                        modifier = Modifier.size(40.dp))
                                }
                            }
                        }
                    }
                    Text("PayOffline", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth())
                    Text("Version 2.0.0", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider()
                    Text(
                        "PayOffline enables fast, secure offline UPI payments using USSD (*99#). " +
                        "No internet connection required — works even in areas with only basic 2G coverage.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    HorizontalDivider()
                    InfoRow("Technology", "USSD *99# (NPCI Standard)")
                    InfoRow("Supported SIMs", "Dual SIM, all Indian carriers")
                    InfoRow("Min Android", "Android 8.0 (API 26)")
                    HorizontalDivider()
                    Text(
                        "Built with ♥ in India",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("Close") }
            }
        )
    }

    // Clear recipients dialog
    if (showClearRecipients) {
        AlertDialog(
            onDismissRequest = { showClearRecipients = false },
            title = { Text("Clear Saved Recipients") },
            text  = { Text("Remove all ${settings.savedRecipients.size} saved recipients?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.updateSettings(settings.copy(savedRecipients = emptyList()))
                    showClearRecipients = false
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearRecipients = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    dangerColor: Boolean = false
) {
    val textColor = if (dangerColor) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val iconTint  = if (dangerColor) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = textColor)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

