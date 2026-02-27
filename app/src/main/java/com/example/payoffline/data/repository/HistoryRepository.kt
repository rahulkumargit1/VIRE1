package com.example.payoffline.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.payoffline.data.model.Transaction
import com.example.payoffline.data.model.TransactionStatus
import com.example.payoffline.data.model.TransactionType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("payoffline_history", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val json = prefs.getString("transactions", "[]") ?: "[]"
        val type = object : TypeToken<List<Transaction>>() {}.type
        val list: List<Transaction> = try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) { emptyList() }
        _transactions.value = list.sortedByDescending { it.timestamp }
    }

    suspend fun addTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        val current = _transactions.value.toMutableList()
        current.add(0, transaction)
        // Keep max 200 records
        val trimmed = if (current.size > 200) current.subList(0, 200) else current
        _transactions.value = trimmed
        prefs.edit().putString("transactions", gson.toJson(trimmed)).apply()
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        _transactions.value = emptyList()
        prefs.edit().remove("transactions").apply()
    }

    /**
     * Export transaction history as a CSV file and share it
     */
    suspend fun exportToCsv(): Uri? = withContext(Dispatchers.IO) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val sb = StringBuilder()
            sb.appendLine("Date,Type,Recipient,Amount,SIM,Status,Response")
            _transactions.value.forEach { tx ->
                val date = sdf.format(Date(tx.timestamp))
                val type = tx.type.name.replace("_", " ")
                val status = tx.status.name
                sb.appendLine("\"$date\",\"$type\",\"${tx.recipient}\",\"${if (tx.amount > 0) "₹${tx.amount}" else "-"}\",\"${tx.simName}\",\"$status\",\"${tx.response.replace("\"", "'")}\"")
            }

            val fileName = "payoffline_history_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            file.writeText(sb.toString())

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            null
        }
    }

    fun shareHistory(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "PayOffline Transaction History")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Export History").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
