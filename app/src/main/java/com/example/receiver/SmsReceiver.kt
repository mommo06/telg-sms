package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.example.data.local.AppDatabase
import com.example.data.repository.TelegramSmsRepository
import com.example.service.CommandProcessor
import com.example.service.TelegramForwarder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

    val messages: Array<SmsMessage>? = Telephony.Sms.Intents.getMessagesFromIntent(intent)
    if (messages.isNullOrEmpty()) return

    val sender = messages[0].originatingAddress ?: "Unknown"
    val fullBody = messages.joinToString("") { it.messageBody ?: "" }
    val timestamp = messages[0].timestampMillis

    val subId = intent.getIntExtra("subscription", -1)
    val simSlot = if (subId >= 0) subId % 2 else 0

    // Check if this is a remote SMS control command
    if (fullBody.startsWith("/")) {
      CoroutineScope(Dispatchers.IO).launch {
        val db = AppDatabase.getDatabase(context)
        val repository = TelegramSmsRepository(db.telegramSmsDao())
        val config = repository.getConfigDirect()

        if (config.isRemoteControlEnabled) {
          val response = CommandProcessor.processCommand(context, fullBody)
          repository.forwardMessage(
            type = "COMMAND",
            title = "SMS Command from $sender",
            rawContent = "$fullBody\n\nResponse:\n$response",
            formattedTelegramText = "⚡ <b>[SMS Remote Command]</b>\nFrom: <code>$sender</code>\nCommand: <code>$fullBody</code>\n\n<b>Result:</b>\n$response",
            simSlot = simSlot
          )
          return@launch
        }

        // Standard forward
        TelegramForwarder.forwardSms(context, sender, fullBody, simSlot, timestamp)
      }
    } else {
      TelegramForwarder.forwardSms(context, sender, fullBody, simSlot, timestamp)
    }
  }
}
