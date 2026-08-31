package com.example.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class TelegramTestResult(
  val success: Boolean,
  val botName: String = "",
  val username: String = "",
  val latencyMs: Long = 0L,
  val errorMessage: String = "",
  val targetChatCount: Int = 0,
  val deliveredChatCount: Int = 0
)

data class TelegramSendResult(
  val success: Boolean,
  val messageId: Long = 0L,
  val errorMessage: String = "",
  val totalChats: Int = 1,
  val deliveredChats: Int = if (success) 1 else 0
)

class TelegramApiClient {

  companion object {
    fun parseChatIds(raw: String): List<String> {
      return raw
        .split(",", ";", "\n", " ", "\t")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
    }
  }

  private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .build()

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  private fun sanitizeUrl(url: String): String {
    val trimmed = url.trim()
    return if (trimmed.endsWith("/")) trimmed.dropLast(1) else trimmed
  }

  suspend fun testBot(token: String, rawChatIds: String, apiUrl: String = "https://api.telegram.org"): TelegramTestResult {
    return withContext(Dispatchers.IO) {
      if (token.isBlank()) {
        return@withContext TelegramTestResult(false, errorMessage = "Bot token is empty")
      }
      val startTime = System.currentTimeMillis()
      val base = if (apiUrl.isBlank()) "https://api.telegram.org" else sanitizeUrl(apiUrl)
      val getMeUrl = "$base/bot$token/getMe"

      try {
        val request = Request.Builder()
          .url(getMeUrl)
          .get()
          .build()

        client.newCall(request).execute().use { response ->
          val elapsed = System.currentTimeMillis() - startTime
          val bodyString = response.body?.string() ?: ""

          if (!response.isSuccessful || bodyString.isBlank()) {
            return@withContext TelegramTestResult(
              success = false,
              latencyMs = elapsed,
              errorMessage = "HTTP ${response.code}: $bodyString"
            )
          }

          val json = JSONObject(bodyString)
          val ok = json.optBoolean("ok", false)
          if (!ok) {
            val desc = json.optString("description", "Unknown error from Telegram API")
            return@withContext TelegramTestResult(
              success = false,
              latencyMs = elapsed,
              errorMessage = desc
            )
          }

          val resultObj = json.optJSONObject("result")
          val botName = resultObj?.optString("first_name", "Telegram Bot") ?: "Telegram Bot"
          val username = resultObj?.optString("username", "") ?: ""

          val chatIds = parseChatIds(rawChatIds)
          var deliveredCount = 0

          // If Chat IDs are provided, send a brief test verification ping to each
          if (chatIds.isNotEmpty()) {
            val testMsg = "🤖 <b>Telegram SMS Connected</b>\n\nApp successfully verified connection to bot @$username.\nTarget chats configured: ${chatIds.size}\n🕒 Timestamp: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}"
            for (id in chatIds) {
              val sendRes = sendMessage(token, id, testMsg, "HTML", base)
              if (sendRes.success) {
                deliveredCount++
              }
            }
          }

          TelegramTestResult(
            success = true,
            botName = botName,
            username = username,
            latencyMs = elapsed,
            targetChatCount = chatIds.size,
            deliveredChatCount = deliveredCount
          )
        }
      } catch (e: Exception) {
        TelegramTestResult(
          success = false,
          errorMessage = e.message ?: "Failed to connect to Telegram server"
        )
      }
    }
  }

  suspend fun sendMessage(
    token: String,
    chatId: String,
    text: String,
    parseMode: String = "HTML",
    apiUrl: String = "https://api.telegram.org"
  ): TelegramSendResult {
    return withContext(Dispatchers.IO) {
      if (token.isBlank() || chatId.isBlank()) {
        return@withContext TelegramSendResult(false, errorMessage = "Bot token or Chat ID is missing")
      }

      val base = if (apiUrl.isBlank()) "https://api.telegram.org" else sanitizeUrl(apiUrl)
      val sendUrl = "$base/bot$token/sendMessage"

      try {
        val payload = JSONObject().apply {
          put("chat_id", chatId.trim())
          put("text", text)
          if (parseMode.isNotBlank()) {
            put("parse_mode", parseMode)
          }
          put("disable_web_page_preview", true)
        }

        val request = Request.Builder()
          .url(sendUrl)
          .post(payload.toString().toRequestBody(jsonMediaType))
          .build()

        client.newCall(request).execute().use { response ->
          val bodyString = response.body?.string() ?: ""
          if (!response.isSuccessful) {
            return@withContext TelegramSendResult(
              success = false,
              errorMessage = "HTTP ${response.code} (Chat $chatId): $bodyString",
              totalChats = 1,
              deliveredChats = 0
            )
          }

          val json = JSONObject(bodyString)
          val ok = json.optBoolean("ok", false)
          if (!ok) {
            val desc = json.optString("description", "Error sending message")
            return@withContext TelegramSendResult(false, errorMessage = "Chat $chatId: $desc", totalChats = 1, deliveredChats = 0)
          }

          val msgObj = json.optJSONObject("result")
          val messageId = msgObj?.optLong("message_id", 0L) ?: 0L
          TelegramSendResult(success = true, messageId = messageId, totalChats = 1, deliveredChats = 1)
        }
      } catch (e: Exception) {
        TelegramSendResult(
          success = false,
          errorMessage = "Chat $chatId: ${e.message ?: "Network error"}",
          totalChats = 1,
          deliveredChats = 0
        )
      }
    }
  }

  suspend fun sendToMultipleChats(
    token: String,
    chatIds: List<String>,
    text: String,
    parseMode: String = "HTML",
    apiUrl: String = "https://api.telegram.org"
  ): TelegramSendResult {
    if (chatIds.isEmpty()) {
      return TelegramSendResult(false, errorMessage = "No destination Chat IDs configured")
    }
    if (chatIds.size == 1) {
      return sendMessage(token, chatIds.first(), text, parseMode, apiUrl)
    }

    return withContext(Dispatchers.IO) {
      val results = coroutineScope {
        chatIds.map { id ->
          async {
            sendMessage(token, id, text, parseMode, apiUrl)
          }
        }.awaitAll()
      }

      val successful = results.filter { it.success }
      val failed = results.filter { !it.success }
      val firstSuccessMsgId = successful.firstOrNull()?.messageId ?: 0L

      if (successful.isNotEmpty()) {
        val errorSummary = if (failed.isNotEmpty()) {
          " (Partial: ${successful.size}/${chatIds.size} sent. Failed: ${failed.joinToString { it.errorMessage }})"
        } else {
          ""
        }
        TelegramSendResult(
          success = true,
          messageId = firstSuccessMsgId,
          errorMessage = errorSummary,
          totalChats = chatIds.size,
          deliveredChats = successful.size
        )
      } else {
        TelegramSendResult(
          success = false,
          errorMessage = failed.joinToString("; ") { it.errorMessage },
          totalChats = chatIds.size,
          deliveredChats = 0
        )
      }
    }
  }

  suspend fun fetchUpdates(
    token: String,
    offset: Long = 0L,
    apiUrl: String = "https://api.telegram.org"
  ): List<TelegramUpdate> {
    return withContext(Dispatchers.IO) {
      if (token.isBlank()) return@withContext emptyList()
      val base = if (apiUrl.isBlank()) "https://api.telegram.org" else sanitizeUrl(apiUrl)
      val url = "$base/bot$token/getUpdates?offset=$offset&timeout=5"

      try {
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) return@withContext emptyList()
          val bodyString = response.body?.string() ?: return@withContext emptyList()
          val json = JSONObject(bodyString)
          if (!json.optBoolean("ok", false)) return@withContext emptyList()

          val results = json.optJSONArray("result") ?: return@withContext emptyList()
          val list = mutableListOf<TelegramUpdate>()
          for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val updateId = item.optLong("update_id", 0L)
            val msg = item.optJSONObject("message")
            if (msg != null) {
              val text = msg.optString("text", "")
              val from = msg.optJSONObject("from")
              val senderId = from?.optLong("id", 0L) ?: 0L
              val senderName = from?.optString("first_name", "User") ?: "User"
              val chatId = msg.optJSONObject("chat")?.optLong("id", 0L) ?: senderId
              list.add(TelegramUpdate(updateId, text, senderId.toString(), chatId.toString(), senderName))
            }
          }
          list
        }
      } catch (e: Exception) {
        emptyList()
      }
    }
  }
}

data class TelegramUpdate(
  val updateId: Long,
  val text: String,
  val senderId: String,
  val chatId: String,
  val senderName: String
)
