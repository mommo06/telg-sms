package com.example.data.network

import com.example.data.model.ForwardConfigEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class CarbonCopyClient {

  private val client = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .build()

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun forwardToAllCarbonCopies(
    config: ForwardConfigEntity,
    title: String,
    content: String
  ): List<String> {
    if (!config.isCarbonCopyEnabled) return emptyList()

    val results = mutableListOf<String>()

    withContext(Dispatchers.IO) {
      // 1. Bark
      if (config.barkUrl.isNotBlank()) {
        try {
          var url = config.barkUrl.trim()
          if (!url.endsWith("/")) url += "/"
          val encodedTitle = URLEncoder.encode(title, "UTF-8")
          val encodedContent = URLEncoder.encode(content, "UTF-8")
          val barkTarget = "$url$encodedTitle/$encodedContent"
          val req = Request.Builder().url(barkTarget).get().build()
          client.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) results.add("Bark: OK") else results.add("Bark: HTTP ${resp.code}")
          }
        } catch (e: Exception) {
          results.add("Bark failed: ${e.message}")
        }
      }

      // 2. PushDeer
      if (config.pushDeerKey.isNotBlank()) {
        try {
          val pushDeerUrl = "https://api2.pushdeer.com/message/push"
          val json = JSONObject().apply {
            put("pushkey", config.pushDeerKey.trim())
            put("text", title)
            put("desp", content)
            put("type", "text")
          }
          val req = Request.Builder()
            .url(pushDeerUrl)
            .post(json.toString().toRequestBody(jsonMediaType))
            .build()
          client.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) results.add("PushDeer: OK") else results.add("PushDeer: HTTP ${resp.code}")
          }
        } catch (e: Exception) {
          results.add("PushDeer failed: ${e.message}")
        }
      }

      // 3. Gotify
      if (config.gotifyUrl.isNotBlank() && config.gotifyToken.isNotBlank()) {
        try {
          var gotifyBase = config.gotifyUrl.trim()
          if (gotifyBase.endsWith("/")) gotifyBase = gotifyBase.dropLast(1)
          val target = "$gotifyBase/message?token=${config.gotifyToken.trim()}"
          val json = JSONObject().apply {
            put("title", title)
            put("message", content)
            put("priority", 5)
          }
          val req = Request.Builder()
            .url(target)
            .post(json.toString().toRequestBody(jsonMediaType))
            .build()
          client.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) results.add("Gotify: OK") else results.add("Gotify: HTTP ${resp.code}")
          }
        } catch (e: Exception) {
          results.add("Gotify failed: ${e.message}")
        }
      }

      // 4. Custom Webhook
      if (config.customWebhookUrl.isNotBlank()) {
        try {
          val json = JSONObject().apply {
            put("title", title)
            put("content", content)
            put("timestamp", System.currentTimeMillis())
            put("source", "TelegramSMS_Android")
          }
          val req = Request.Builder()
            .url(config.customWebhookUrl.trim())
            .post(json.toString().toRequestBody(jsonMediaType))
            .build()
          client.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) results.add("Custom Webhook: OK") else results.add("Custom Webhook: HTTP ${resp.code}")
          }
        } catch (e: Exception) {
          results.add("Custom Webhook failed: ${e.message}")
        }
      }
    }

    return results
  }
}
