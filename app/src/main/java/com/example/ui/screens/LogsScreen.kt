package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LogEntity
import com.example.ui.components.DeliveryStatusBadge
import com.example.ui.components.TypeBadge
import com.example.ui.theme.StatusError
import com.example.ui.theme.TelegramBlue
import com.example.ui.viewmodel.TelegramSmsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(
  viewModel: TelegramSmsViewModel,
  modifier: Modifier = Modifier
) {
  val logs by viewModel.filteredLogs.collectAsStateWithLifecycle()
  val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
  val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

  var showClearConfirmDialog by remember { mutableStateOf(false) }
  val context = LocalContext.current

  val filters = listOf(
    "ALL" to "All Events (${allLogs.size})",
    "SMS" to "SMS (${allLogs.count { it.type == "SMS" }})",
    "CALL" to "Calls (${allLogs.count { it.type == "CALL" }})",
    "BATTERY" to "Battery (${allLogs.count { it.type == "BATTERY" }})",
    "COMMAND" to "Commands (${allLogs.count { it.type == "COMMAND" }})",
    "CARBON_COPY" to "Carbon Copy (${allLogs.count { it.type == "CARBON_COPY" }})"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    // Top Bar Actions
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        "Forwarding Logs",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
      )

      if (allLogs.isNotEmpty()) {
        IconButton(
          onClick = { showClearConfirmDialog = true },
          modifier = Modifier.testTag("clear_logs_button")
        ) {
          Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All Logs", tint = StatusError)
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Search Field
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { viewModel.setSearchQuery(it) },
      placeholder = { Text("Search logs by sender, content, error...") },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TelegramBlue) },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { viewModel.setSearchQuery("") }) {
            Icon(Icons.Default.Clear, contentDescription = "Clear Search")
          }
        }
      },
      shape = RoundedCornerShape(12.dp),
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("logs_search_input")
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Filter Chips Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      filters.forEach { (type, label) ->
        val isSelected = selectedFilter == type
        Surface(
          color = if (isSelected) TelegramBlue else MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier
            .clickable { viewModel.setFilter(type) }
            .testTag("filter_chip_$type")
        ) {
          Text(
            text = label,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Log list or Empty State
    if (logs.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.FilterList, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            if (searchQuery.isNotEmpty() || selectedFilter != "ALL") "No matching logs found" else "No forwarded messages recorded yet",
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            "Incoming SMS, calls, or battery alerts will be listed here with real-time status.",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(logs, key = { it.id }) { log ->
          DetailedLogCard(
            log = log,
            onResend = { viewModel.resendLog(log) },
            onDelete = { viewModel.deleteLog(log.id) },
            onCopy = {
              val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
              val clip = ClipData.newPlainText("Telegram SMS Log", "${log.title}\n\n${log.content}\n\n${log.extraInfo}")
              cm?.setPrimaryClip(clip)
              Toast.makeText(context, "Log copied to clipboard", Toast.LENGTH_SHORT).show()
            }
          )
        }
      }
    }
  }

  if (showClearConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showClearConfirmDialog = false },
      title = { Text("Clear All Logs?") },
      text = { Text("This will permanently delete all forwarding logs and history from the local database.") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.clearAllLogs()
            showClearConfirmDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = StatusError)
        ) {
          Text("Delete All")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirmDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun DetailedLogCard(
  log: LogEntity,
  onResend: () -> Unit,
  onDelete: () -> Unit,
  onCopy: () -> Unit
) {
  var isExpanded by remember { mutableStateOf(false) }
  val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))

  ElevatedCard(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          TypeBadge(type = log.type)
          Spacer(modifier = Modifier.width(6.dp))
          if (log.simSlot >= 0) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                "SIM ${log.simSlot + 1}",
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
              )
            }
          }
        }
        DeliveryStatusBadge(status = log.status)
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = log.title,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = log.content,
        fontSize = 13.sp,
        maxLines = if (isExpanded) Int.MAX_VALUE else 3,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      if (log.extraInfo.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = log.extraInfo,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(6.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(dateStr, fontSize = 11.sp, color = Color.Gray)

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TelegramBlue, modifier = Modifier.size(16.dp))
          }

          if (log.status == "FAILED") {
            IconButton(onClick = onResend, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Default.Refresh, contentDescription = "Resend", tint = StatusError, modifier = Modifier.size(18.dp))
            }
          }

          IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
          }

          TextButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.height(32.dp)
          ) {
            Text(if (isExpanded) "Less" else "More", fontSize = 11.sp, color = TelegramBlue)
          }
        }
      }
    }
  }
}
