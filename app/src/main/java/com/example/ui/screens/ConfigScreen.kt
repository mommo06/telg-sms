package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BotTestResultCard
import com.example.ui.components.ImportExportConfigDialog
import com.example.ui.theme.TelegramBlue
import com.example.ui.viewmodel.TelegramSmsViewModel

@Composable
fun ConfigScreen(
  viewModel: TelegramSmsViewModel,
  modifier: Modifier = Modifier
) {
  val config by viewModel.config.collectAsStateWithLifecycle()
  val botTestState by viewModel.botTestState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  var botToken by remember(config.botToken) { mutableStateOf(config.botToken) }
  var chatId by remember(config.chatId) { mutableStateOf(config.chatId) }
  var customApiUrl by remember(config.customApiUrl) { mutableStateOf(config.customApiUrl) }

  var isSmsForwardEnabled by remember(config.isSmsForwardEnabled) { mutableStateOf(config.isSmsForwardEnabled) }
  var smsTemplate by remember(config.smsFormatTemplate) { mutableStateOf(config.smsFormatTemplate) }

  var isCallForwardEnabled by remember(config.isCallForwardEnabled) { mutableStateOf(config.isCallForwardEnabled) }
  var callTemplate by remember(config.callFormatTemplate) { mutableStateOf(config.callFormatTemplate) }

  var isBatteryForwardEnabled by remember(config.isBatteryForwardEnabled) { mutableStateOf(config.isBatteryForwardEnabled) }
  var batteryThreshold by remember(config.batteryThreshold) { mutableFloatStateOf(config.batteryThreshold.toFloat()) }
  var isChargingAlertEnabled by remember(config.isChargingAlertEnabled) { mutableStateOf(config.isChargingAlertEnabled) }

  var showToken by remember { mutableStateOf(false) }
  var showImportExportDialog by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    // Header & Actions
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        "Bot & Forwarding Setup",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
      )

      IconButton(onClick = { showImportExportDialog = true }) {
        Icon(Icons.Default.QrCode, contentDescription = "Import/Export QR/JSON", tint = TelegramBlue)
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 1. Telegram Bot Credentials Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Key, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Telegram Bot Credentials", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bot Token Field
        OutlinedTextField(
          value = botToken,
          onValueChange = { botToken = it },
          label = { Text("Bot Token") },
          placeholder = { Text("123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ") },
          visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            Row {
              IconButton(onClick = { showToken = !showToken }) {
                Icon(
                  if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = "Toggle token visibility"
                )
              }
              IconButton(onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = cm?.primaryClip
                if (clip != null && clip.itemCount > 0) {
                  botToken = clip.getItemAt(0).text.toString().trim()
                }
              }) {
                Icon(Icons.Default.ContentPaste, contentDescription = "Paste Token", tint = TelegramBlue)
              }
            }
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("bot_token_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        val parsedChatIds = remember(chatId) { com.example.data.network.TelegramApiClient.parseChatIds(chatId) }

        // Chat ID Field (Supports single or multiple IDs)
        OutlinedTextField(
          value = chatId,
          onValueChange = { chatId = it },
          label = {
            Text(
              if (parsedChatIds.size > 1) "Destination Chat IDs (${parsedChatIds.size} chats)"
              else "Destination Chat IDs (Multiple supported)"
            )
          },
          placeholder = { Text("e.g. 123456789, -100123456789, 87654321") },
          trailingIcon = {
            Row {
              IconButton(onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = cm?.primaryClip
                if (clip != null && clip.itemCount > 0) {
                  val pasted = clip.getItemAt(0).text.toString().trim()
                  chatId = if (chatId.isBlank()) pasted else "$chatId, $pasted"
                }
              }) {
                Icon(Icons.Default.ContentPaste, contentDescription = "Paste Chat ID", tint = TelegramBlue)
              }
              IconButton(onClick = {
                Toast.makeText(
                  context,
                  "You can enter multiple Chat IDs separated by commas or spaces. Personal IDs: e.g. 12345678. Group/Channel IDs: e.g. -100123456789. Get IDs via @userinfobot.",
                  Toast.LENGTH_LONG
                ).show()
              }) {
                Icon(Icons.Default.HelpOutline, contentDescription = "Help")
              }
            }
          },
          supportingText = {
            Text("Separate multiple Chat IDs with commas (,) or spaces. Messages forward to all recipients.", fontSize = 11.sp)
          },
          modifier = Modifier.fillMaxWidth().testTag("chat_id_input")
        )

        // Parsed Chat ID Badges
        if (parsedChatIds.isNotEmpty()) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            "Active Target Recipients (${parsedChatIds.size}):",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            parsedChatIds.forEach { id ->
              val isGroupOrChannel = id.startsWith("-")
              Surface(
                color = if (isGroupOrChannel) TelegramBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    if (isGroupOrChannel) Icons.Default.Group else Icons.Default.Person,
                    contentDescription = null,
                    tint = if (isGroupOrChannel) TelegramBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = id,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = if (isGroupOrChannel) TelegramBlue else MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove $id",
                    modifier = Modifier
                      .size(14.dp)
                      .clickable {
                        val remaining = parsedChatIds.filter { it != id }
                        chatId = remaining.joinToString(", ")
                      },
                    tint = Color.Gray
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Custom API Endpoint
        OutlinedTextField(
          value = customApiUrl,
          onValueChange = { customApiUrl = it },
          label = { Text("Telegram Bot API URL (Optional self-host)") },
          placeholder = { Text("https://api.telegram.org") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Test Connection & Save Action
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = {
              viewModel.testTelegramBot(botToken, chatId, customApiUrl)
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f).testTag("test_bot_button")
          ) {
            Icon(Icons.Default.NetworkPing, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Test Bot API")
          }

          Button(
            onClick = {
              viewModel.updateConfig(
                config.copy(
                  botToken = botToken.trim(),
                  chatId = chatId.trim(),
                  customApiUrl = customApiUrl.trim(),
                  isSmsForwardEnabled = isSmsForwardEnabled,
                  smsFormatTemplate = smsTemplate,
                  isCallForwardEnabled = isCallForwardEnabled,
                  callFormatTemplate = callTemplate,
                  isBatteryForwardEnabled = isBatteryForwardEnabled,
                  batteryThreshold = batteryThreshold.toInt(),
                  isChargingAlertEnabled = isChargingAlertEnabled
                )
              )
              Toast.makeText(context, "Configuration saved successfully!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f).testTag("save_config_button")
          ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save")
          }
        }

        // Test result banner
        BotTestResultCard(
          state = botTestState,
          onDismiss = { viewModel.resetBotTestState() }
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 2. SMS Forwarding Rules Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Message, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("SMS Forwarding", fontWeight = FontWeight.Bold, fontSize = 16.sp)
          }
          Switch(
            checked = isSmsForwardEnabled,
            onCheckedChange = { isSmsForwardEnabled = it },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TelegramBlue)
          )
        }

        if (isSmsForwardEnabled) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            "Custom SMS Format Template (Supports {sender}, {sim}, {time}, {content}):",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = smsTemplate,
            onValueChange = { smsTemplate = it },
            minLines = 3,
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 3. Call Forwarding Rules Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Call, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Missed Call Forwarding", fontWeight = FontWeight.Bold, fontSize = 16.sp)
          }
          Switch(
            checked = isCallForwardEnabled,
            onCheckedChange = { isCallForwardEnabled = it },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TelegramBlue)
          )
        }

        if (isCallForwardEnabled) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            "Custom Call Format Template (Supports {caller}, {sim}, {time}):",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = callTemplate,
            onValueChange = { callTemplate = it },
            minLines = 2,
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 4. Battery Alerts Rules Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Battery & Power Alerts", fontWeight = FontWeight.Bold, fontSize = 16.sp)
          }
          Switch(
            checked = isBatteryForwardEnabled,
            onCheckedChange = { isBatteryForwardEnabled = it },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TelegramBlue)
          )
        }

        if (isBatteryForwardEnabled) {
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            "Low Battery Threshold: ${batteryThreshold.toInt()}%",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
          )
          Slider(
            value = batteryThreshold,
            onValueChange = { batteryThreshold = it },
            valueRange = 5f..50f,
            steps = 8,
            colors = SliderDefaults.colors(thumbColor = TelegramBlue, activeTrackColor = TelegramBlue)
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Notify on Charging Plug / Unplug", fontSize = 13.sp)
            Switch(
              checked = isChargingAlertEnabled,
              onCheckedChange = { isChargingAlertEnabled = it },
              colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TelegramBlue)
            )
          }
        }
      }
    }
  }

  if (showImportExportDialog) {
    ImportExportConfigDialog(
      configJson = viewModel.exportConfigJson(),
      onDismiss = { showImportExportDialog = false },
      onImport = { json -> viewModel.importConfigJson(json) }
    )
  }
}
