package com.example.payoffline.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.payoffline.data.model.UiState
import com.example.payoffline.ui.components.*
import com.example.payoffline.ui.theme.Emerald600
import com.example.payoffline.ui.theme.Indigo600
import com.example.payoffline.viewmodel.UssdViewModel

@Composable
fun BalanceScreen(vm: UssdViewModel) {
    val balanceUiState by vm.balanceUiState.collectAsState()
    val selectedSim    by vm.selectedSim.collectAsState()
    val sims           by vm.sims.collectAsState()

    var pin by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val scrollState  = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        GradientHeaderCard(
            title    = "Balance & Services",
            subtitle = "Enquiry via *99#",
            icon     = Icons.Filled.AccountBalance
        )

        // SIM info
        if (selectedSim != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SIM Card", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SimSelectorChip(sim = selectedSim, onClick = { vm.setShowSimSelector(true) })
            }
        }

        // PIN field
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter UPI PIN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Your PIN is required to fetch balance and statements.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                PayTextField(
                    value         = pin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                    label         = "UPI PIN",
                    placeholder   = "4–6 digit PIN",
                    leadingIcon   = Icons.Filled.Lock,
                    keyboardType  = KeyboardType.NumberPassword,
                    imeAction     = ImeAction.Done,
                    onImeAction   = { focusManager.clearFocus() },
                    isPassword    = true
                )
            }
        }

        // Status
        StatusBanner(
            uiState = balanceUiState,
            onDialerFallback = { vm.openDialerFallback("balance") },
            onDismiss = vm::clearBalanceResponse
        )

        // Action buttons
        Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                icon    = Icons.Filled.AccountBalance,
                label   = "Check Balance",
                onClick = {
                    focusManager.clearFocus()
                    vm.checkBalance(pin)
                },
                color = MaterialTheme.colorScheme.primaryContainer,
                tint  = MaterialTheme.colorScheme.primary
            )
            QuickActionButton(
                icon    = Icons.Filled.Receipt,
                label   = "Mini Statement",
                onClick = {
                    focusManager.clearFocus()
                    vm.miniStatement(pin)
                },
                color = Color(0xFFFEF3C7),
                tint  = Color(0xFFD97706)
            )
            QuickActionButton(
                icon    = Icons.Filled.AddLink,
                label   = "Link Bank",
                onClick = {
                    focusManager.clearFocus()
                    vm.linkBankAccount(pin)
                },
                color = Color(0xFFDBEAFE),
                tint  = Color(0xFF1D4ED8)
            )
        }

        // Large action buttons
        Button(
            onClick = {
                focusManager.clearFocus()
                vm.checkBalance(pin)
            },
            enabled  = balanceUiState !is UiState.Loading && sims.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Emerald600)
        ) {
            if (balanceUiState is UiState.Loading) {
                CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Fetching…")
            } else {
                Icon(Icons.Filled.AccountBalance, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Check Balance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        OutlinedButton(
            onClick = {
                focusManager.clearFocus()
                vm.miniStatement(pin)
            },
            enabled  = balanceUiState !is UiState.Loading && sims.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape    = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Filled.Receipt, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Mini Statement", style = MaterialTheme.typography.titleSmall)
        }

        OutlinedButton(
            onClick = {
                focusManager.clearFocus()
                vm.linkBankAccount(pin)
            },
            enabled  = balanceUiState !is UiState.Loading && sims.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape    = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Filled.AddLink, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Link Bank Account", style = MaterialTheme.typography.titleSmall)
        }

        // Dialer fallback info
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp).padding(top = 1.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "If the request fails, the app will automatically open the Dialer with the USSD code pre-filled. Just tap the call button.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}
