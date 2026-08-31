package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusPurple
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TelegramAccent
import com.example.ui.theme.TelegramBlue
import com.example.ui.viewmodel.BotTestUiState

@Composable
fun StatusPulseIndicator(isActive: Boolean) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.85f,
    targetValue = 1.25f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseScale"
  )

  Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
    if (isActive) {
      Box(
        modifier = Modifier
          .size(20.dp)
          .scale(pulseScale)
          .clip(CircleShape)
          .background(StatusSuccess.copy(alpha = 0.35f))
      )
    }
    Box(
      modifier = Modifier
        .size(10.dp)
        .clip(CircleShape)
        .background(if (isActive) StatusSuccess else Color.Gray)
    )
  }
}

@Composable
fun TypeBadge(type: String) {
  val (bgColor, textColor, icon, label) = when (type.uppercase()) {
    "SMS" -> Quadruple(StatusInfo.copy(alpha = 0.15f), StatusInfo, Icons.Default.Message, "SMS")
    "CALL" -> Quadruple(StatusWarning.copy(alpha = 0.15f), StatusWarning, Icons.Default.Call, "CALL")
    "BATTERY" -> Quadruple(StatusPurple.copy(alpha = 0.15f), StatusPurple, Icons.Default.BatteryChargingFull, "BATTERY")
    "COMMAND" -> Quadruple(Color(0xFF26A69A).copy(alpha = 0.15f), Color(0xFF00897B), Icons.Default.Terminal, "COMMAND")
    "CARBON_COPY" -> Quadruple(TelegramAccent.copy(alpha = 0.15f), TelegramBlue, Icons.Default.CloudUpload, "CARBON COPY")
    else -> Quadruple(Color.Gray.copy(alpha = 0.15f), Color.DarkGray, Icons.Default.Info, type)
  }

  Surface(
    color = bgColor,
    shape = RoundedCornerShape(6.dp),
    modifier = Modifier.padding(vertical = 2.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(12.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = label,
        color = textColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
fun DeliveryStatusBadge(status: String) {
  val (color, text) = when (status) {
    "SUCCESS" -> Pair(StatusSuccess, "DELIVERED")
    "FAILED" -> Pair(StatusError, "FAILED")
    "PENDING" -> Pair(StatusWarning, "PENDING")
    "RETRIED" -> Pair(TelegramAccent, "RETRIED")
    else -> Pair(Color.Gray, status)
  }

  Surface(
    color = color.copy(alpha = 0.15f),
    shape = RoundedCornerShape(6.dp)
  ) {
    Text(
      text = text,
      color = color,
      fontSize = 10.sp,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
  }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun BotTestResultCard(state: BotTestUiState, onDismiss: () -> Unit) {
  when (state) {
    is BotTestUiState.Idle -> Unit
    is BotTestUiState.Loading -> {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
          Spacer(modifier = Modifier.width(12.dp))
          Text("Testing connection to Telegram Bot API...", style = MaterialTheme.typography.bodyMedium)
        }
      }
    }
    is BotTestUiState.Success -> {
      Card(
        colors = CardDefaults.cardColors(containerColor = StatusSuccess.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              "Connection Successful!",
              color = StatusSuccess,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text("• Bot Name: ${state.botName}", fontSize = 13.sp)
          if (state.username.isNotBlank()) {
            Text("• Username: @${state.username}", fontSize = 13.sp)
          }
          if (state.targetChatCount > 0) {
            Text("• Destination Chats: ${state.deliveredChatCount}/${state.targetChatCount} test message(s) delivered", fontSize = 13.sp)
          }
          Text("• API Latency: ${state.latencyMs} ms", fontSize = 13.sp)
          Spacer(modifier = Modifier.height(8.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) {
              Text("Dismiss")
            }
          }
        }
      }
    }
    is BotTestUiState.Error -> {
      Card(
        colors = CardDefaults.cardColors(containerColor = StatusError.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Error, contentDescription = null, tint = StatusError)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              "Connection Failed",
              color = StatusError,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(state.message, color = StatusError, fontSize = 13.sp)
          Spacer(modifier = Modifier.height(8.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) {
              Text("Dismiss")
            }
          }
        }
      }
    }
  }
}

@Composable
fun QuickSimulateEventDialog(
  onDismiss: () -> Unit,
  onSendSms: (sender: String, message: String, simSlot: Int) -> Unit,
  onSendCall: (caller: String, simSlot: Int) -> Unit,
  onSendBattery: (level: Int, isCharging: Boolean) -> Unit
) {
  var selectedTab by remember { mutableIntStateOf(0) } // 0: SMS, 1: Call, 2: Battery
  var senderInput by remember { mutableStateOf("+1 800-555-0199") }
  var messageInput by remember { mutableStateOf("Your verification code is 849201. Do not share this with anyone.") }
  var simSlotInput by remember { mutableIntStateOf(0) }
  var batteryLevel by remember { mutableIntStateOf(12) }
  var isCharging by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Simulate & Test Event Dispatch") },
    text = {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          listOf("SMS Message", "Missed Call", "Battery Alert").forEachIndexed { index, label ->
            Surface(
              color = if (selectedTab == index) TelegramBlue else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.clickable { selectedTab = index }
            ) {
              Text(
                text = label,
                color = if (selectedTab == index) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
              )
            }
          }
        }

        when (selectedTab) {
          0 -> {
            OutlinedTextField(
              value = senderInput,
              onValueChange = { senderInput = it },
              label = { Text("Sender Phone / ID") },
              modifier = Modifier.fillMaxWidth().testTag("sim_sender_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = messageInput,
              onValueChange = { messageInput = it },
              label = { Text("SMS Content") },
              minLines = 2,
              modifier = Modifier.fillMaxWidth().testTag("sim_message_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("SIM Slot: ", fontSize = 13.sp)
              Spacer(modifier = Modifier.width(8.dp))
              listOf("SIM 1" to 0, "SIM 2" to 1).forEach { (label, slot) ->
                Surface(
                  color = if (simSlotInput == slot) TelegramBlue.copy(alpha = 0.2f) else Color.Transparent,
                  shape = RoundedCornerShape(6.dp),
                  modifier = Modifier.clickable { simSlotInput = slot }.padding(end = 6.dp)
                ) {
                  Text(
                    text = label,
                    color = if (simSlotInput == slot) TelegramBlue else MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(6.dp)
                  )
                }
              }
            }
          }
          1 -> {
            OutlinedTextField(
              value = senderInput,
              onValueChange = { senderInput = it },
              label = { Text("Caller Number / Name") },
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Simulates a missed incoming call and dispatches the alert to Telegram & Carbon Copy.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          2 -> {
            OutlinedTextField(
              value = batteryLevel.toString(),
              onValueChange = { batteryLevel = it.toIntOrNull() ?: 15 },
              label = { Text("Battery Level (%)") },
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("Charging state: ", fontSize = 13.sp)
              TextButton(onClick = { isCharging = !isCharging }) {
                Text(if (isCharging) "⚡ Charging" else "🪫 Discharging")
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          when (selectedTab) {
            0 -> onSendSms(senderInput, messageInput, simSlotInput)
            1 -> onSendCall(senderInput, simSlotInput)
            2 -> onSendBattery(batteryLevel, isCharging)
          }
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
        modifier = Modifier.testTag("sim_dispatch_confirm_button")
      ) {
        Text("Dispatch Now")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun DirectSmsDialog(
  onDismiss: () -> Unit,
  onSend: (destination: String, text: String) -> String
) {
  var destination by remember { mutableStateOf("") }
  var text by remember { mutableStateOf("") }
  var statusMessage by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Send SMS from Device") },
    text = {
      Column {
        Text(
          "Sends a real SMS directly from your device using standard carrier telephony.",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = destination,
          onValueChange = { destination = it },
          label = { Text("Recipient Phone Number") },
          placeholder = { Text("+1 234 567 8900") },
          modifier = Modifier.fillMaxWidth().testTag("direct_sms_phone_input")
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          label = { Text("Message Body") },
          minLines = 3,
          modifier = Modifier.fillMaxWidth().testTag("direct_sms_body_input")
        )
        if (statusMessage != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(statusMessage ?: "", color = TelegramBlue, fontSize = 12.sp)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (destination.isNotBlank() && text.isNotBlank()) {
            val res = onSend(destination, text)
            statusMessage = res
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
        modifier = Modifier.testTag("direct_sms_send_button")
      ) {
        Text("Send SMS")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}

@Composable
fun ImportExportConfigDialog(
  configJson: String,
  onDismiss: () -> Unit,
  onImport: (String) -> Boolean
) {
  var editJson by remember { mutableStateOf(configJson) }
  var importResult by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Backup & Restore Configuration") },
    text = {
      Column {
        Text(
          "You can copy this JSON configuration for backup, or paste a new config JSON (or QR export) below:",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = editJson,
          onValueChange = { editJson = it },
          minLines = 8,
          maxLines = 12,
          textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
          modifier = Modifier.fillMaxWidth().testTag("config_json_field")
        )
        if (importResult != null) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(importResult ?: "", color = TelegramBlue, fontSize = 12.sp)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val success = onImport(editJson)
          if (success) {
            importResult = "Config imported successfully!"
          } else {
            importResult = "Failed to parse JSON. Check syntax."
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
      ) {
        Text("Apply Config")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}
