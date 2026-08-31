package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "forward_config")
data class ForwardConfigEntity(
  @PrimaryKey
  val id: Int = 1,
  val botToken: String = "",
  val chatId: String = "",
  val trustedChatId: String = "",
  val customApiUrl: String = "https://api.telegram.org",
  val isSmsForwardEnabled: Boolean = true,
  val isCallForwardEnabled: Boolean = true,
  val isBatteryForwardEnabled: Boolean = true,
  val batteryThreshold: Int = 15,
  val isChargingAlertEnabled: Boolean = true,
  val isCarbonCopyEnabled: Boolean = false,
  val barkUrl: String = "",
  val pushDeerKey: String = "",
  val gotifyUrl: String = "",
  val gotifyToken: String = "",
  val customWebhookUrl: String = "",
  val smsFormatTemplate: String = "📩 [Telegram-SMS]\nSIM: {sim}\nFrom: {sender}\nTime: {time}\n\n{content}",
  val callFormatTemplate: String = "📞 [Telegram-SMS]\nMissed Call\nFrom: {caller}\nSIM: {sim}\nTime: {time}",
  val isRemoteControlEnabled: Boolean = true,
  val isServiceRunning: Boolean = false,
  val lastPingLatencyMs: Long = -1L,
  val botUsername: String = ""
)
