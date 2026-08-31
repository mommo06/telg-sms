package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ForwardToInbox
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.ForwardConfigEntity
import com.example.data.model.LogEntity
import com.example.ui.components.DeliveryStatusBadge
import com.example.ui.components.DirectSmsDialog
import com.example.ui.components.QuickSimulateEventDialog
import com.example.ui.components.StatusPulseIndicator
import com.example.ui.components.TypeBadge
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TelegramAccent
import com.example.ui.theme.TelegramBlue
import com.example.ui.viewmodel.TelegramSmsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
  viewModel: TelegramSmsViewModel,
  onNavigateToLogs: () -> Unit,
  onNavigateToConfig: () -> Unit,
  modifier: Modifier = Modifier
) {
  val config by viewModel.config.collectAsStateWithLifecycle()
  val logs by viewModel.allLogs.collectAsStateWithLifecycle()
  val simCards by viewModel.simCards.collectAsStateWithLifecycle()
  val batteryInfo by viewModel.batteryInfo.collectAsStateWithLifecycle()
  val networkInfo by viewModel.networkInfo.collectAsStateWithLifecycle()
  val isServiceActive by viewModel.isServiceActive.collectAsStateWithLifecycle()

  var showSimulateDialog by remember { mutableStateOf(false) }
  var showDirectSmsDialog by remember { mutableStateOf(false) }

  val recentLogs = remember(logs) { logs.take(4) }
  val parsedChatIds = remember(config.chatId) { com.example.data.network.TelegramApiClient.parseChatIds(config.chatId) }
  val isConfigured = config.botToken.isNotBlank() && parsedChatIds.isNotEmpty()

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    // 1. Hero Mascot & Banner Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp)),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
      Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
        Image(
          painter = painterResource(id = R.drawable.hero_telegram_sms_banner_1788184768001),
          contentDescription = "Telegram SMS Assistant Banner",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.horizontalGradient(
                colors = listOf(Navy900.copy(alpha = 0.85f), Color.Transparent)
              )
            )
        )
        Column(
          modifier = Modifier
            .align(Alignment.CenterStart)
            .padding(16.dp)
        ) {
          Surface(
            color = TelegramBlue,
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(
              "TELEGRAM SMS GATEWAY",
              color = Color.White,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            "Seamless SMS & Call Forwarding",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            when {
              !isConfigured -> "Bot not configured yet"
              parsedChatIds.size > 1 -> "Connected to ${config.botUsername.ifEmpty { "Telegram Bot" }} (${parsedChatIds.size} chats)"
              else -> "Connected to ${config.botUsername.ifEmpty { "Telegram Bot" }}"
            },
            color = Color(0xFFB0BEC5),
            fontSize = 12.sp
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 2. Master Service Status Card
    ElevatedCard(
      modifier = Modifier.fillMaxWidth().testTag("service_status_card"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.elevatedCardColors(
        containerColor = if (isServiceActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
      )
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            StatusPulseIndicator(isActive = isServiceActive)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = if (isServiceActive) "Forwarding Service Active" else "Forwarding Service Idle",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (isServiceActive) "Listening for SMS, Calls & Battery changes" else "Tap toggle switch to activate daemon",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Switch(
            checked = isServiceActive,
            onCheckedChange = { viewModel.toggleService(it) },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TelegramBlue),
            modifier = Modifier.testTag("service_toggle_switch")
          )
        }

        if (!isConfigured) {
          Spacer(modifier = Modifier.height(12.dp))
          Surface(
            color = StatusWarning.copy(alpha = 0.12f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Warning, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                "Bot Token or Chat ID is missing. Please complete setup in Bot Config.",
                color = StatusWarning,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
              )
              TextButton(onClick = onNavigateToConfig) {
                Text("Setup", color = TelegramBlue, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 3. Telemetry Grid: SIM Cards & Battery / Network Status
    Text(
      "Device & Network Telemetry",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      // SIM Card Card
      Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SimCard, contentDescription = null, tint = TelegramAccent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("SIM Status", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
          Spacer(modifier = Modifier.height(6.dp))
          if (simCards.isNotEmpty()) {
            simCards.take(2).forEach { sim ->
              Text(
                "[SIM ${sim.slotIndex + 1}] ${sim.carrierName}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          } else {
            Text("Dual-SIM Ready", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }

      // Battery & Power Card
      Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              if (batteryInfo.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.Bolt,
              contentDescription = null,
              tint = if (batteryInfo.isCharging) StatusSuccess else StatusInfo,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Power & Temp", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            "${batteryInfo.level}% • ${batteryInfo.pluggedType}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            "${batteryInfo.temperatureCelsius}°C • ${networkInfo.networkType}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 4. Quick Action Toolbar: Simulate SMS/Call, Direct SMS, Refresh
    Text(
      "Quick Actions & Testing",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = { showSimulateDialog = true },
        colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.weight(1f).testTag("simulate_event_button")
      ) {
        Icon(Icons.Default.ForwardToInbox, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Simulate", fontSize = 12.sp)
      }

      OutlinedButton(
        onClick = { showDirectSmsDialog = true },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.weight(1f).testTag("direct_sms_button")
      ) {
        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Send SMS", fontSize = 12.sp)
      }

      IconButton(
        onClick = { viewModel.refreshDeviceStatus() },
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant)
      ) {
        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TelegramBlue)
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 5. Recent Forwarding Activity Feed
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        "Recent Forwarding Activity",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )
      TextButton(onClick = onNavigateToLogs) {
        Text("View All (${logs.size})", color = TelegramBlue, fontWeight = FontWeight.SemiBold)
      }
    }

    if (recentLogs.isEmpty()) {
      Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(Icons.Default.Message, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
          Spacer(modifier = Modifier.height(8.dp))
          Text("No forwarding events yet", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            "Tap 'Simulate' above or receive a real SMS to see forwarded logs here.",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )
        }
      }
    } else {
      recentLogs.forEach { log ->
        RecentLogCard(log = log, onResend = { viewModel.resendLog(log) })
        Spacer(modifier = Modifier.height(8.dp))
      }
    }
  }

  // Dialogs
  if (showSimulateDialog) {
    QuickSimulateEventDialog(
      onDismiss = { showSimulateDialog = false },
      onSendSms = { sender, msg, slot -> viewModel.sendTestSmsForward(sender, msg, slot) },
      onSendCall = { caller, slot -> viewModel.sendTestCallForward(caller, slot) },
      onSendBattery = { lvl, chg -> viewModel.sendTestBatteryForward(lvl, chg) }
    )
  }

  if (showDirectSmsDialog) {
    DirectSmsDialog(
      onDismiss = { showDirectSmsDialog = false },
      onSend = { dest, text -> viewModel.sendDirectSms(dest, text) }
    )
  }
}

@Composable
fun RecentLogCard(log: LogEntity, onResend: () -> Unit) {
  val dateStr = SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
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

      Spacer(modifier = Modifier.height(4.dp))
      Text(log.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
      Text(
        log.content,
        fontSize = 12.sp,
        maxLines = 2,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(4.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(dateStr, fontSize = 10.sp, color = Color.Gray)
        if (log.status == "FAILED") {
          TextButton(onClick = onResend, modifier = Modifier.height(28.dp)) {
            Text("Retry", fontSize = 11.sp, color = StatusError)
          }
        }
      }
    }
  }
}
