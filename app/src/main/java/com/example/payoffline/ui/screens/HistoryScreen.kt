package com.example.payoffline.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.payoffline.data.model.Transaction
import com.example.payoffline.data.model.TransactionStatus
import com.example.payoffline.data.model.TransactionType
import com.example.payoffline.ui.components.TransactionCard
import com.example.payoffline.ui.theme.Emerald600
import com.example.payoffline.ui.theme.Rose500
import com.example.payoffline.viewmodel.UssdViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: UssdViewModel) {
    val transactions by vm.transactions.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    var filterType   by remember { mutableStateOf<TransactionType?>(null) }
    var filterStatus by remember { mutableStateOf<TransactionStatus?>(null) }

    val filtered = remember(transactions, filterType, filterStatus) {
        transactions.filter { tx ->
            (filterType == null || tx.type == filterType) &&
            (filterStatus == null || tx.status == filterStatus)
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Top bar
        Surface(tonalElevation = 3.dp) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Transaction History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.exportHistory() }) {
                    Icon(Icons.Filled.FileDownload, "Export", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(Icons.Filled.DeleteOutline, "Clear", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = filterType == null && filterStatus == null,
                    onClick  = { filterType = null; filterStatus = null },
                    label    = { Text("All") }
                )
            }
            items(TransactionType.values()) { type ->
                FilterChip(
                    selected = filterType == type,
                    onClick  = { filterType = if (filterType == type) null else type },
                    label    = { Text(type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
            item {
                FilterChip(
                    selected = filterStatus == TransactionStatus.SUCCESS,
                    onClick  = { filterStatus = if (filterStatus == TransactionStatus.SUCCESS) null else TransactionStatus.SUCCESS },
                    label    = { Text("Success") },
                    leadingIcon = { Icon(Icons.Filled.CheckCircle, null, Modifier.size(14.dp), tint = Emerald600) }
                )
            }
            item {
                FilterChip(
                    selected = filterStatus == TransactionStatus.FAILED,
                    onClick  = { filterStatus = if (filterStatus == TransactionStatus.FAILED) null else TransactionStatus.FAILED },
                    label    = { Text("Failed") },
                    leadingIcon = { Icon(Icons.Filled.Cancel, null, Modifier.size(14.dp), tint = Rose500) }
                )
            }
        }

        // Stats row
        if (transactions.isNotEmpty()) {
            val success = transactions.count { it.status == TransactionStatus.SUCCESS }
            val failed  = transactions.count { it.status == TransactionStatus.FAILED }
            val total   = transactions.filter { it.amount > 0 }.sumOf { it.amount }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatChip("Total", "${transactions.size}", Modifier.weight(1f))
                StatChip("Success", "$success", Modifier.weight(1f), color = MaterialTheme.colorScheme.secondaryContainer)
                StatChip("Failed", "$failed", Modifier.weight(1f), color = MaterialTheme.colorScheme.errorContainer)
            }
        }

        // List
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.ReceiptLong, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                    Text("No transactions yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Your payment history will appear here", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { tx ->
                    AnimatedVisibility(visible = true, enter = fadeIn() + expandVertically()) {
                        TransactionCard(tx)
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // Clear confirm dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon  = { Icon(Icons.Filled.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear History") },
            text  = { Text("This will permanently delete all transaction history. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { vm.clearHistory(); showClearDialog = false },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Clear All") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Surface(color = color, shape = RoundedCornerShape(10.dp), modifier = modifier) {
        Column(
            Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
