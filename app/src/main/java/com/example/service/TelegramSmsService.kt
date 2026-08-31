package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.network.TelegramApiClient
import com.example.data.repository.TelegramSmsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TelegramSmsService : Service() {

  companion object {
    const val CHANNEL_ID = "telegram_sms_service_channel"
    const val NOTIFICATION_ID = 1001
    const val ACTION_STOP_SERVICE = "com.example.ACTION_STOP_SERVICE"
    var isRunning = false
      private set
  }

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var commandPollingJob: Job? = null
  private var lastUpdateId = 0L
  private var lastBatteryLevel = -1
  private var wasCharging = false

  private val batteryReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val currentPct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
          status == BatteryManager.BATTERY_STATUS_FULL

        val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280)
        val tempCelsius = tempRaw / 10f

        serviceScope.launch {
          val db = AppDatabase.getDatabase(context)
          val repository = TelegramSmsRepository(db.telegramSmsDao())
          val config = repository.getConfigDirect()

          if (!config.isBatteryForwardEnabled) return@launch

          // 1. Check low battery threshold
          if (currentPct in 1..config.batteryThreshold && lastBatteryLevel > config.batteryThreshold) {
            TelegramForwarder.forwardBattery(context, currentPct, isCharging, tempCelsius)
          }

          // 2. Check charging state transition if enabled
          if (config.isChargingAlertEnabled && lastBatteryLevel != -1 && wasCharging != isCharging) {
            TelegramForwarder.forwardBattery(context, currentPct, isCharging, tempCelsius)
          }

          lastBatteryLevel = currentPct
          wasCharging = isCharging
        }
      }
    }
  }

  override fun onCreate() {
    super.onCreate()
    isRunning = true
    createNotificationChannel()
    startForeground(NOTIFICATION_ID, buildNotification("Service is active & listening"))

    // Register battery receiver
    registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    // Start background remote command polling if bot token is present
    startPollingLoop()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP_SERVICE) {
      stopForeground(STOP_FOREGROUND_REMOVE)
      stopSelf()
      return START_NOT_STICKY
    }
    return START_STICKY
  }

  private fun startPollingLoop() {
    commandPollingJob?.cancel()
    commandPollingJob = serviceScope.launch {
      val telegramClient = TelegramApiClient()
      val db = AppDatabase.getDatabase(this@TelegramSmsService)
      val repository = TelegramSmsRepository(db.telegramSmsDao(), telegramClient)

      while (isActive) {
        try {
          val config = repository.getConfigDirect()
          if (config.botToken.isNotBlank() && config.isRemoteControlEnabled) {
            val updates = telegramClient.fetchUpdates(
              token = config.botToken,
              offset = lastUpdateId + 1,
              apiUrl = config.customApiUrl
            )

            for (update in updates) {
              lastUpdateId = maxOf(lastUpdateId, update.updateId)
              val text = update.text.trim()

              // Verify trusted chat ID or destination chat IDs if configured
              val trustedIds = TelegramApiClient.parseChatIds(config.trustedChatId)
              val destinationIds = TelegramApiClient.parseChatIds(config.chatId)
              val isAuthorized = (trustedIds.isEmpty() && destinationIds.isEmpty()) ||
                trustedIds.contains(update.chatId) ||
                trustedIds.contains(update.senderId) ||
                destinationIds.contains(update.chatId) ||
                destinationIds.contains(update.senderId)

              if (text.startsWith("/") && isAuthorized) {
                val commandResponse = CommandProcessor.processCommand(this@TelegramSmsService, text)
                repository.forwardMessage(
                  type = "COMMAND",
                  title = "Bot Command: ${text.take(30)}",
                  rawContent = "Command: $text\nFrom: ${update.senderName} (${update.senderId})\n\nResponse:\n$commandResponse",
                  formattedTelegramText = "🤖 <b>[Bot Command Reply]</b>\n\n<b>Command:</b> <code>$text</code>\n<b>User:</b> ${update.senderName}\n\n$commandResponse",
                  simSlot = -1
                )
              }
            }
          }
        } catch (e: Exception) {
          // Ignore network glitch during polling
        }
        delay(8000) // Poll interval
      }
    }
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "Telegram SMS Gateway Service",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Monitors SMS, calls and battery status for Telegram forwarding"
        setShowBadge(false)
      }
      val manager = getSystemService(NotificationManager::class.java)
      manager.createNotificationChannel(channel)
    }
  }

  private fun buildNotification(statusText: String): Notification {
    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java),
      PendingIntent.FLAG_IMMUTABLE
    )

    val stopIntent = PendingIntent.getService(
      this,
      1,
      Intent(this, TelegramSmsService::class.java).apply { action = ACTION_STOP_SERVICE },
      PendingIntent.FLAG_IMMUTABLE
    )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("Telegram SMS Running")
      .setContentText(statusText)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentIntent(pendingIntent)
      .setOngoing(true)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
      .build()
  }

  override fun onDestroy() {
    isRunning = false
    try {
      unregisterReceiver(batteryReceiver)
    } catch (e: Exception) {
      // Receiver may not have been registered
    }
    commandPollingJob?.cancel()
    serviceScope.cancel()
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null
}
