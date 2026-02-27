package com.example.payoffline.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.payoffline.data.model.*
import com.example.payoffline.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ─── Gradient Header Card ─────────────────────────────────────────────────────
@Composable
fun GradientHeaderCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.linearGradient(listOf(Indigo500, Indigo700))
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

// ─── SIM Selector Chip ───────────────────────────────────────────────────────
@Composable
fun SimSelectorChip(sim: SimInfo?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(50.dp)).clickable(onClick = onClick),
        color    = MaterialTheme.colorScheme.primaryContainer,
        shape    = RoundedCornerShape(50.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.SimCard, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(sim?.displayName ?: "Select SIM", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
    }
}

// ─── Pay TextField ───────────────────────────────────────────────────────────
@Composable
fun PayTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    isPassword: Boolean = false,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var showPassword by remember { mutableStateOf(false) }
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        placeholder   = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
        leadingIcon   = leadingIcon?.let { icon -> { Icon(icon, null, modifier = Modifier.size(20.dp)) } },
        trailingIcon  = if (isPassword) {{
            IconButton(onClick = { showPassword = !showPassword }) {
                Icon(if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null, modifier = Modifier.size(20.dp))
            }
        }} else trailingIcon,
        visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        singleLine  = true,
        enabled     = enabled,
        shape       = RoundedCornerShape(14.dp),
        modifier    = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

// ─── Status Banner ────────────────────────────────────────────────────────────
@Composable
fun StatusBanner(uiState: UiState, onDialerFallback: (() -> Unit)? = null, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = uiState !is UiState.Idle,
        enter   = slideInVertically() + fadeIn(),
        exit    = slideOutVertically() + fadeOut()
    ) {
        when (uiState) {
            is UiState.Loading -> Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Connecting via *99#…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            is UiState.Success -> Surface(color = Color(0xFFD1FAE5), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Emerald600, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Success", style = MaterialTheme.typography.labelLarge.copy(color = Color(0xFF065F46)))
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, null, Modifier.size(16.dp), tint = Color(0xFF065F46))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(uiState.message, style = MaterialTheme.typography.bodySmall, color = Color(0xFF065F46))
                }
            }
            is UiState.Error -> Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ErrorOutline, null, tint = Rose600, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Failed", style = MaterialTheme.typography.labelLarge.copy(color = Color(0xFF9B1C1C)))
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, null, Modifier.size(16.dp), tint = Color(0xFF9B1C1C))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(uiState.message, style = MaterialTheme.typography.bodySmall, color = Color(0xFF9B1C1C))
                    if (onDialerFallback != null &&
                        (uiState.errorType == UssdErrorType.CARRIER_NOT_SUPPORTED ||
                         uiState.errorType == UssdErrorType.NETWORK_FAILURE ||
                         uiState.errorType == UssdErrorType.PERMISSION_DENIED)) {
                        Spacer(Modifier.height(10.dp))
                        TextButton(onClick = onDialerFallback, colors = ButtonDefaults.textButtonColors(contentColor = Rose600)) {
                            Icon(Icons.Filled.Phone, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Try via Dialer instead", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

// ─── Transaction Card ─────────────────────────────────────────────────────────
@Composable
fun TransactionCard(tx: Transaction) {
    val sdf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val date = remember(tx.timestamp) { sdf.format(Date(tx.timestamp)) }
    val (icon, iconBg, iconTint) = when (tx.type) {
        TransactionType.SEND_MONEY     -> Triple(Icons.Filled.Send, Color(0xFFEDE9FE), Indigo600)
        TransactionType.CHECK_BALANCE  -> Triple(Icons.Filled.AccountBalance, Color(0xFFD1FAE5), Emerald600)
        TransactionType.MINI_STATEMENT -> Triple(Icons.Filled.Receipt, Color(0xFFFEF3C7), Amber500)
        TransactionType.LINK_BANK      -> Triple(Icons.Filled.AddLink, Color(0xFFDBEAFE), Color(0xFF1D4ED8))
    }
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tx.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleSmall)
                if (tx.recipient.isNotBlank()) Text("To: ${tx.recipient}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (tx.amount > 0) Text("₹${tx.amount.toInt()}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (tx.status == TransactionStatus.SUCCESS) Emerald600 else Rose500)
                Spacer(Modifier.height(4.dp))
                StatusPill(tx.status)
            }
        }
    }
}

@Composable
fun StatusPill(status: TransactionStatus) {
    val (label, bg, fg) = when (status) {
        TransactionStatus.SUCCESS -> Triple("Success", Color(0xFFD1FAE5), Emerald600)
        TransactionStatus.FAILED  -> Triple("Failed",  Color(0xFFFEE2E2), Rose600)
        TransactionStatus.PENDING -> Triple("Pending", Color(0xFFFEF3C7), Amber500)
    }
    Box(modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(bg).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg)
    }
}

// ─── Permission Banner ────────────────────────────────────────────────────────
@Composable
fun PermissionBanner(onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Surface(color = Color(0xFFFFF7ED), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(14.dp))) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WarningAmber, null, tint = Amber500, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Phone Permission Needed", style = MaterialTheme.typography.labelLarge, color = Color(0xFF92400E))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "CALL_PHONE permission enables automatic USSD requests. Without it, the app will use the Dialer as a fallback — you'll need to tap Call manually.",
                style = MaterialTheme.typography.bodySmall, color = Color(0xFF92400E)
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRequest,
                    colors  = ButtonDefaults.buttonColors(containerColor = Amber500),
                    modifier = Modifier.height(36.dp)
                ) { Text("Grant Permission", style = MaterialTheme.typography.labelMedium) }
                TextButton(onClick = onOpenSettings) {
                    Text("Open Settings", style = MaterialTheme.typography.labelMedium, color = Color(0xFF92400E))
                }
            }
        }
    }
}

// ─── Quick Action Button ──────────────────────────────────────────────────────
@Composable
fun QuickActionButton(icon: ImageVector, label: String, onClick: () -> Unit, color: Color = MaterialTheme.colorScheme.primaryContainer, tint: Color = MaterialTheme.colorScheme.primary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(color), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
