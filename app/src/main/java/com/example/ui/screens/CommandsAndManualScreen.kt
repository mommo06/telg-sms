package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TelegramAccent
import com.example.ui.theme.TelegramBlue
import com.example.ui.viewmodel.ConsoleMessage
import com.example.ui.viewmodel.TelegramSmsViewModel

@Composable
fun CommandsAndManualScreen(
  viewModel: TelegramSmsViewModel,
  modifier: Modifier = Modifier
) {
  val config by viewModel.config.collectAsStateWithLifecycle()
  val consoleMessages by viewModel.consoleMessages.collectAsStateWithLifecycle()
  val context = LocalContext.current

  var commandInput by remember { mutableStateOf("") }
  var isRemoteControlEnabled by remember(config.isRemoteControlEnabled) { mutableStateOf(config.isRemoteControlEnabled) }
  var trustedChatId by remember(config.trustedChatId) { mutableStateOf(config.trustedChatId) }

  val quickCommands = listOf("/ping", "/battery", "/sim", "/info", "/help")

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    Text(
      "Remote Control & Terminal",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold
    )
    Text(
      "Send SMS and check device status remotely via Telegram Bot commands or SMS commands.",
      fontSize = 13.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
    )

    // Remote Control Settings Card
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
            Icon(Icons.Default.Security, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Enable Remote Control", fontWeight = FontWeight.Bold, fontSize = 15.sp)
          }
          Switch(
            checked = isRemoteControlEnabled,
            onCheckedChange = {
              isRemoteControlEnabled = it
              viewModel.updateConfig(config.copy(isRemoteControlEnabled = it))
            },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TelegramBlue),
            modifier = Modifier.testTag("remote_control_toggle")
          )
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = trustedChatId,
          onValueChange = {
            trustedChatId = it
            viewModel.updateConfig(config.copy(trustedChatId = it.trim()))
          },
          label = { Text("Trusted Admin User/Chat IDs (Optional)") },
          placeholder = { Text("e.g. 123456789, 987654321 (leave blank to allow destination chats)") },
          supportingText = {
            Text("Separate multiple trusted admin IDs with commas or spaces.", fontSize = 11.sp)
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("trusted_chat_id_input")
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Interactive Command Terminal Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Navy900)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Terminal, contentDescription = null, tint = TelegramAccent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Command Test Console", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          }
          Surface(
            color = StatusSuccess.copy(alpha = 0.2f),
            shape = RoundedCornerShape(4.dp)
          ) {
            Text("ONLINE", color = StatusSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Action Chips
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          quickCommands.forEach { cmd ->
            Surface(
              color = Navy800,
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.padding(vertical = 2.dp)
            ) {
              Text(
                text = cmd,
                color = TelegramAccent,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                  .padding(horizontal = 8.dp, vertical = 4.dp)
                  .clickable {
                    viewModel.executeConsoleCommand(cmd)
                  }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Console Output Box
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Navy800, RoundedCornerShape(8.dp))
            .padding(10.dp)
        ) {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            reverseLayout = false
          ) {
            items(consoleMessages) { msg ->
              Column(modifier = Modifier.padding(vertical = 3.dp)) {
                Text(
                  text = if (msg.sender == "USER") "> ${msg.text}" else msg.text.replace("<b>", "").replace("</b>", "").replace("<code>", "").replace("</code>", ""),
                  color = if (msg.sender == "USER") Color(0xFF81D4FA) else Color(0xFFE0E0E0),
                  fontFamily = FontFamily.Monospace,
                  fontSize = 11.5.sp,
                  lineHeight = 16.sp
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Terminal Input Field
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = commandInput,
            onValueChange = { commandInput = it },
            placeholder = { Text("Type command (e.g. /ping, /sim)...", color = Color.Gray, fontSize = 12.sp) },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
            modifier = Modifier.weight(1f).testTag("terminal_input_field")
          )
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (commandInput.isNotBlank()) {
                viewModel.executeConsoleCommand(commandInput.trim())
                commandInput = ""
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("terminal_execute_button")
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Run", modifier = Modifier.size(16.dp))
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // User Manual & Guide Section
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.HelpOutline, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("User Manual & Setup Instructions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        GuideItem(
          step = "1",
          title = "Create a Telegram Bot",
          desc = "Open Telegram, search for @BotFather, send /newbot, follow the prompts, and copy your HTTP API token."
        )

        GuideItem(
          step = "2",
          title = "Get Chat IDs (Multiple supported)",
          desc = "Message @userinfobot on Telegram to get your Chat ID. You can enter multiple Chat IDs (personal, groups, or channels) separated by commas to forward messages to multiple chats simultaneously."
        )

        GuideItem(
          step = "3",
          title = "Disable Battery Optimization",
          desc = "In Android Settings > Apps > Telegram SMS > Battery, select 'Unrestricted' so background forwarding continues reliably."
        )

        GuideItem(
          step = "4",
          title = "Dual-SIM Support",
          desc = "Telegram SMS identifies SIM 1 and SIM 2 slots automatically for both incoming SMS and call notifications."
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Open Source & Character Attribution Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Info, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("About & Open Source Credits", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          "• License: BSD 3-Clause License\n" +
          "• Artwork: Character Fay (菲, フェイ) by @walliant (CC BY-NC-SA 4.0)\n" +
          "• Stack: Kotlin, Jetpack Compose, Room, OkHttp, Moshi, Coroutines\n" +
          "• Config QR generator: config.telegram-sms.com",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 18.sp
        )
      }
    }
  }
}

@Composable
fun GuideItem(step: String, title: String, desc: String) {
  Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
    Surface(
      color = TelegramBlue,
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.size(24.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text(step, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }
    Spacer(modifier = Modifier.width(10.dp))
    Column {
      Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
      Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
