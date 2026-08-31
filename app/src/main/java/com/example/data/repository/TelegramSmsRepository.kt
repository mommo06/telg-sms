package com.example.data.repository

import com.example.data.local.TelegramSmsDao
import com.example.data.model.ForwardConfigEntity
import com.example.data.model.LogEntity
import com.example.data.network.CarbonCopyClient
import com.example.data.network.TelegramApiClient
import com.example.data.network.TelegramSendResult
import com.example.data.network.TelegramTestResult
import kotlinx.coroutines.flow.Flow

class TelegramSmsRepository(
  private val dao: TelegramSmsDao,
  private val telegramClient: TelegramApiClient = TelegramApiClient(),
  private val carbonCopyClient: CarbonCopyClient = CarbonCopyClient()
) {

  val logs: Flow<List<LogEntity>> = dao.getAllLogs()
  val config: Flow<ForwardConfigEntity?> = dao.getConfigFlow()

  suspend fun getConfigDirect(): ForwardConfigEntity {
    return dao.getConfigDirect() ?: ForwardConfigEntity()
  }

  suspend fun saveConfig(config: ForwardConfigEntity) {
    dao.saveConfig(config)
  }

  suspend fun insertLog(log: LogEntity): Long {
    return dao.insertLog(log)
  }

  suspend fun updateLog(log: LogEntity) {
    dao.updateLog(log)
  }

  suspend fun deleteLog(id: Long) {
    dao.deleteLogById(id)
  }

  suspend fun clearLogs() {
    dao.clearAllLogs()
  }

  suspend fun clearAllLogs() {
    dao.clearAllLogs()
  }

  suspend fun testTelegramBot(token: String, chatId: String, apiUrl: String): TelegramTestResult {
    val result = telegramClient.testBot(token, chatId, apiUrl)
    if (result.success) {
      val currentConfig = getConfigDirect()
      saveConfig(
        currentConfig.copy(
          botToken = token,
          chatId = chatId,
          customApiUrl = apiUrl,
          lastPingLatencyMs = result.latencyMs,
          botUsername = result.username
        )
      )
    }
    return result
  }

  suspend fun forwardMessage(
    type: String,
    title: String,
    rawContent: String,
    formattedTelegramText: String,
    simSlot: Int = -1
  ): TelegramSendResult {
    val config = getConfigDirect()
    val chatIds = TelegramApiClient.parseChatIds(config.chatId)

    // 1. Send to Telegram (All configured Chat IDs)
    var sendResult = TelegramSendResult(false, errorMessage = "Service is not configured")
    if (config.botToken.isNotBlank() && chatIds.isNotEmpty()) {
      sendResult = telegramClient.sendToMultipleChats(
        token = config.botToken,
        chatIds = chatIds,
        text = formattedTelegramText,
        parseMode = "HTML",
        apiUrl = config.customApiUrl
      )
    }

    // 2. Dispatch to Carbon Copy if enabled
    val carbonResults = if (config.isCarbonCopyEnabled) {
      carbonCopyClient.forwardToAllCarbonCopies(config, title, rawContent)
    } else {
      emptyList()
    }

    val extraInfo = buildString {
      if (sendResult.success) {
        if (sendResult.totalChats > 1) {
          append("Telegram: Delivered to ${sendResult.deliveredChats}/${sendResult.totalChats} chats (Msg #${sendResult.messageId})")
        } else {
          append("Telegram: Delivered (Msg #${sendResult.messageId})")
        }
        if (sendResult.errorMessage.isNotBlank()) {
          append(sendResult.errorMessage)
        }
      } else {
        append("Telegram Error: ${sendResult.errorMessage}")
      }
      if (carbonResults.isNotEmpty()) {
        append("\nCC: ")
        append(carbonResults.joinToString(", "))
      }
    }

    val logStatus = if (sendResult.success) "SUCCESS" else "FAILED"

    // 3. Save to database
    val logEntity = LogEntity(
      type = type,
      title = title,
      content = rawContent,
      timestamp = System.currentTimeMillis(),
      simSlot = simSlot,
      status = logStatus,
      extraInfo = extraInfo
    )
    dao.insertLog(logEntity)

    return sendResult
  }

  suspend fun resendLog(log: LogEntity): TelegramSendResult {
    val config = getConfigDirect()
    val chatIds = TelegramApiClient.parseChatIds(config.chatId)

    val sendResult = telegramClient.sendToMultipleChats(
      token = config.botToken,
      chatIds = chatIds,
      text = "🔄 <b>[Telegram-SMS Resend]</b>\n\n<b>${log.title}</b>\n${log.content}",
      parseMode = "HTML",
      apiUrl = config.customApiUrl
    )

    if (sendResult.success) {
      val statusText = if (sendResult.totalChats > 1) {
        "Resent to ${sendResult.deliveredChats}/${sendResult.totalChats} chats (Msg #${sendResult.messageId})"
      } else {
        "Resent successfully (Msg #${sendResult.messageId})"
      }
      dao.updateLog(
        log.copy(
          status = "SUCCESS",
          extraInfo = statusText
        )
      )
    } else {
      dao.updateLog(
        log.copy(
          status = "FAILED",
          extraInfo = "Resend failed: ${sendResult.errorMessage}"
        )
      )
    }
    return sendResult
  }
}
