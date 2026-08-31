package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.TelegramBlue
import com.example.ui.viewmodel.TelegramSmsViewModel

@Composable
fun CarbonCopyScreen(
  viewModel: TelegramSmsViewModel,
  modifier: Modifier = Modifier
) {
  val config by viewModel.config.collectAsStateWithLifecycle()
  val context = LocalContext.current

  var isCarbonCopyEnabled by remember(config.isCarbonCopyEnabled) { mutableStateOf(config.isCarbonCopyEnabled) }
  var barkUrl by remember(config.barkUrl) { mutableStateOf(config.barkUrl) }
  var pushDeerKey by remember(config.pushDeerKey) { mutableStateOf(config.pushDeerKey) }
  var gotifyUrl by remember(config.gotifyUrl) { mutableStateOf(config.gotifyUrl) }
  var gotifyToken by remember(config.gotifyToken) { mutableStateOf(config.gotifyToken) }
  var customWebhookUrl by remember(config.customWebhookUrl) { mutableStateOf(config.customWebhookUrl) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    Text(
      "Carbon Copy Destinations",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold
    )
    Text(
      "Forward SMS and call alerts to backup push notification services in parallel with Telegram.",
      fontSize = 13.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
    )

    // Master Switch Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CloudUpload, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text("Enable Carbon Copy", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Broadcast to external channels", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }

        Switch(
          checked = isCarbonCopyEnabled,
          onCheckedChange = { isCarbonCopyEnabled = it },
          colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TelegramBlue),
          modifier = Modifier.testTag("carbon_copy_master_switch")
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 1. Bark
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Bark (iOS Push Service)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = barkUrl,
          onValueChange = { barkUrl = it },
          label = { Text("Bark Push Server URL") },
          placeholder = { Text("https://api.day.app/YOUR_KEY/") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("bark_url_input")
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 2. PushDeer
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Send, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("PushDeer", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = pushDeerKey,
          onValueChange = { pushDeerKey = it },
          label = { Text("PushDeer PushKey") },
          placeholder = { Text("PDU...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("pushdeer_key_input")
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 3. Gotify
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Cloud, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Gotify (Self-hosted Push)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = gotifyUrl,
          onValueChange = { gotifyUrl = it },
          label = { Text("Gotify Server URL") },
          placeholder = { Text("https://gotify.yourdomain.com") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = gotifyToken,
          onValueChange = { gotifyToken = it },
          label = { Text("Gotify Application Token") },
          placeholder = { Text("A...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 4. Custom Webhook
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Http, contentDescription = null, tint = TelegramBlue, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Custom Webhook (JSON POST)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = customWebhookUrl,
          onValueChange = { customWebhookUrl = it },
          label = { Text("Webhook Endpoint URL") },
          placeholder = { Text("https://webhook.site/...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Save & Test Carbon Copy Actions
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      OutlinedButton(
        onClick = {
          viewModel.sendTestSmsForward(
            sender = "+1 888-CC-TEST",
            message = "Carbon Copy verification test from Telegram SMS Android Gateway.",
            simSlot = 0
          )
          Toast.makeText(context, "Dispatched Carbon Copy test event!", Toast.LENGTH_SHORT).show()
        },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.weight(1f)
      ) {
        Text("Test Dispatch")
      }

      Button(
        onClick = {
          viewModel.updateConfig(
            config.copy(
              isCarbonCopyEnabled = isCarbonCopyEnabled,
              barkUrl = barkUrl.trim(),
              pushDeerKey = pushDeerKey.trim(),
              gotifyUrl = gotifyUrl.trim(),
              gotifyToken = gotifyToken.trim(),
              customWebhookUrl = customWebhookUrl.trim()
            )
          )
          Toast.makeText(context, "Carbon Copy configuration saved!", Toast.LENGTH_SHORT).show()
        },
        colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.weight(1f).testTag("save_cc_config_button")
      ) {
        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Save Config")
      }
    }
  }
}
