package com.example.service

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.repository.TelegramSmsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TelegramForwarder {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  fun forwardSms(
    context: Context,
    sender: String,
    content: String,
    simSlot: Int = 0,
    timestamp: Long = System.currentTimeMillis()
  ) {
    scope.launch {
      val db = AppDatabase.getDatabase(context)
      val repository = TelegramSmsRepository(db.telegramSmsDao())
      val config = repository.getConfigDirect()

      if (!config.isSmsForwardEnabled) return@launch

      val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
      val simText = if (simSlot >= 0) "SIM ${simSlot + 1}" else "SIM 1"

      val template = if (config.smsFormatTemplate.isNotBlank()) config.smsFormatTemplate else
        "📩 <b>[Telegram-SMS]</b>\n<b>From:</b> <code>{sender}</code>\n<b>SIM:</b> {sim}\n<b>Time:</b> {time}\n\n{content}"

      val formattedText = template
        .replace("{sender}", sender)
        .replace("{sim}", simText)
        .replace("{time}", timeStr)
        .replace("{content}", content)

      repository.forwardMessage(
        type = "SMS",
        title = "SMS from $sender ($simText)",
        rawContent = content,
        formattedTelegramText = formattedText,
        simSlot = simSlot
      )
    }
  }

  fun forwardCall(
    context: Context,
    caller: String,
    simSlot: Int = 0,
    timestamp: Long = System.currentTimeMillis()
  ) {
    scope.launch {
      val db = AppDatabase.getDatabase(context)
      val repository = TelegramSmsRepository(db.telegramSmsDao())
      val config = repository.getConfigDirect()

      if (!config.isCallForwardEnabled) return@launch

      val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
      val simText = if (simSlot >= 0) "SIM ${simSlot + 1}" else "SIM 1"

      val template = if (config.callFormatTemplate.isNotBlank()) config.callFormatTemplate else
        "📞 <b>[Telegram-SMS] Missed Call</b>\n<b>Caller:</b> <code>{caller}</code>\n<b>SIM:</b> {sim}\n<b>Time:</b> {time}"

      val formattedText = template
        .replace("{caller}", caller)
        .replace("{sim}", simText)
        .replace("{time}", timeStr)

      repository.forwardMessage(
        type = "CALL",
        title = "Missed Call from $caller ($simText)",
        rawContent = "Missed incoming call from $caller",
        formattedTelegramText = formattedText,
        simSlot = simSlot
      )
    }
  }

  fun forwardBattery(
    context: Context,
    level: Int,
    isCharging: Boolean,
    temperatureCelsius: Float
  ) {
    scope.launch {
      val db = AppDatabase.getDatabase(context)
      val repository = TelegramSmsRepository(db.telegramSmsDao())
      val config = repository.getConfigDirect()

      if (!config.isBatteryForwardEnabled) return@launch

      val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
      val icon = if (isCharging) "⚡" else "🪫"
      val stateText = if (isCharging) "Charging" else "Discharging"

      val formattedText = """
        $icon <b>[Telegram-SMS Battery Alert]</b>
        
        • Battery Level: <b>$level%</b> ($stateText)
        • Temperature: <b>${temperatureCelsius}°C</b>
        • Timestamp: <code>$timeStr</code>
      """.trimIndent()

      repository.forwardMessage(
        type = "BATTERY",
        title = "Battery Alert: $level% ($stateText)",
        rawContent = "Battery $level%, State: $stateText, Temp: ${temperatureCelsius}°C",
        formattedTelegramText = formattedText,
        simSlot = -1
      )
    }
  }
}
