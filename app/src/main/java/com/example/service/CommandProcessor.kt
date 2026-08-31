package com.example.service

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import com.example.utils.DeviceInfoHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CommandProcessor {

  fun processCommand(context: Context, commandText: String): String {
    val trimmed = commandText.trim()
    val parts = trimmed.split("\\s+".toRegex())
    if (parts.isEmpty()) return "Unknown command. Type /help for assistance."

    val rootCommand = parts[0].lowercase(Locale.ROOT)

    return when {
      rootCommand == "/help" || rootCommand == "/start" -> {
        """
        🤖 <b>Telegram-SMS Commands:</b>
        
        • <code>/send &lt;number&gt; &lt;text&gt;</code> - Send an SMS message from device
        • <code>/battery</code> - Query battery level, temperature & charge state
        • <code>/sim</code> - List active SIM cards & carrier info
        • <code>/info</code> - System information & network connectivity
        • <code>/ping</code> - Check device latency & service responsiveness
        • <code>/ussd &lt;code&gt;</code> - Request USSD balance check
        """.trimIndent()
      }

      rootCommand == "/ping" -> {
        val now = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        "🏓 <b>Pong!</b> Device is online and responsive.\nTime: <code>$now</code>"
      }

      rootCommand == "/battery" -> {
        val batt = DeviceInfoHelper.getBatteryInfo(context)
        val icon = if (batt.isCharging) "⚡" else "🔋"
        """
        $icon <b>Device Battery Status:</b>
        • Level: <b>${batt.level}%</b>
        • State: <b>${batt.pluggedType}</b>
        • Temperature: <b>${batt.temperatureCelsius}°C</b>
        • Health: <b>${batt.health}</b>
        """.trimIndent()
      }

      rootCommand == "/sim" -> {
        val sims = DeviceInfoHelper.getSimCards(context)
        val sb = StringBuilder("📶 <b>Active SIM Cards:</b>\n")
        sims.forEach { sim ->
          sb.append("• <b>[SIM ${sim.slotIndex + 1}]</b> ${sim.displayName} (${sim.carrierName}) [${sim.countryIso}]\n")
        }
        sb.toString().trimEnd()
      }

      rootCommand == "/info" -> {
        val net = DeviceInfoHelper.getNetworkInfo(context)
        val batt = DeviceInfoHelper.getBatteryInfo(context)
        """
        📱 <b>Device Information:</b>
        • Model: <b>${Build.MANUFACTURER.uppercase()} ${Build.MODEL}</b>
        • Android Version: <b>Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})</b>
        • Network: <b>${net.networkType}</b>
        • Battery: <b>${batt.level}%</b> (${batt.pluggedType})
        • App: <b>Telegram SMS 2.0 (Compose Edition)</b>
        """.trimIndent()
      }

      rootCommand == "/send" -> {
        if (parts.size < 3) {
          "❌ Usage: <code>/send &lt;phone_number&gt; &lt;message&gt;</code>"
        } else {
          val destination = parts[1]
          val messageBody = parts.drop(2).joinToString(" ")
          sendRealSms(context, destination, messageBody)
        }
      }

      rootCommand == "/ussd" -> {
        if (parts.size < 2) {
          "❌ Usage: <code>/ussd &lt;code&gt;</code> (e.g. <code>/ussd *100#</code>)"
        } else {
          val ussdCode = parts[1]
          "📞 USSD request <code>$ussdCode</code> logged. (Requires direct phone dialer permission)"
        }
      }

      else -> {
        "❓ Unknown command <code>$rootCommand</code>. Type <code>/help</code> for available commands."
      }
    }
  }

  fun sendRealSms(context: Context, destination: String, message: String): String {
    return try {
      val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(SmsManager::class.java)
      } else {
        @Suppress("DEPRECATION")
        SmsManager.getDefault()
      }

      val parts = smsManager.divideMessage(message)
      if (parts.size > 1) {
        smsManager.sendMultipartTextMessage(destination, null, parts, null, null)
      } else {
        smsManager.sendTextMessage(destination, null, message, null, null)
      }
      "✅ SMS successfully queued to <b>$destination</b>:\n\"$message\""
    } catch (e: SecurityException) {
      "⚠️ <b>SMS Failed:</b> SEND_SMS permission is not granted on device."
    } catch (e: Exception) {
      "⚠️ <b>SMS Error:</b> ${e.message ?: "Failed to dispatch SMS"}"
    }
  }
}
