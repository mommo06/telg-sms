package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ForwardConfigEntity
import com.example.data.model.LogEntity
import com.example.data.network.TelegramTestResult
import com.example.data.repository.TelegramSmsRepository
import com.example.service.CommandProcessor
import com.example.service.TelegramForwarder
import com.example.service.TelegramSmsService
import com.example.utils.BatteryInfo
import com.example.utils.DeviceInfoHelper
import com.example.utils.DeviceNetworkInfo
import com.example.utils.SimCardInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed interface BotTestUiState {
  object Idle : BotTestUiState
  object Loading : BotTestUiState
  data class Success(
    val botName: String,
    val username: String,
    val latencyMs: Long,
    val targetChatCount: Int = 0,
    val deliveredChatCount: Int = 0
  ) : BotTestUiState
  data class Error(val message: String) : BotTestUiState
}

data class ConsoleMessage(
  val sender: String, // "USER" or "BOT"
  val text: String,
  val timestamp: Long = System.currentTimeMillis()
)

class TelegramSmsViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: TelegramSmsRepository

  val config: StateFlow<ForwardConfigEntity>
  val allLogs: StateFlow<List<LogEntity>>

  private val _selectedFilter = MutableStateFlow("ALL")
  val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _botTestState = MutableStateFlow<BotTestUiState>(BotTestUiState.Idle)
  val botTestState: StateFlow<BotTestUiState> = _botTestState.asStateFlow()

  private val _simCards = MutableStateFlow<List<SimCardInfo>>(emptyList())
  val simCards: StateFlow<List<SimCardInfo>> = _simCards.asStateFlow()

  private val _batteryInfo = MutableStateFlow(
    BatteryInfo(85, false, 28.5f, "Good", "Discharging")
  )
  val batteryInfo: StateFlow<BatteryInfo> = _batteryInfo.asStateFlow()

  private val _networkInfo = MutableStateFlow(DeviceNetworkInfo(true, "Wi-Fi"))
  val networkInfo: StateFlow<DeviceNetworkInfo> = _networkInfo.asStateFlow()

  private val _isServiceActive = MutableStateFlow(false)
  val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

  private val _consoleMessages = MutableStateFlow<List<ConsoleMessage>>(
    listOf(
      ConsoleMessage("BOT", "🤖 Telegram SMS Remote Command Terminal ready.\nType /help to see available commands or test /ping, /battery, /sim, /send.")
    )
  )
  val consoleMessages: StateFlow<List<ConsoleMessage>> = _consoleMessages.asStateFlow()

  val filteredLogs: StateFlow<List<LogEntity>>

  init {
    val db = AppDatabase.getDatabase(application)
    repository = TelegramSmsRepository(db.telegramSmsDao())

    config = repository.config
      .combine(MutableStateFlow(Unit)) { cfg, _ -> cfg ?: ForwardConfigEntity() }
      .stateIn(viewModelScope, SharingStarted.Eagerly, ForwardConfigEntity())

    allLogs = repository.logs
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    filteredLogs = combine(allLogs, _selectedFilter, _searchQuery) { logs, filter, query ->
      logs.filter { log ->
        val matchesFilter = when (filter) {
          "ALL" -> true
          "SMS" -> log.type == "SMS"
          "CALL" -> log.type == "CALL"
          "BATTERY" -> log.type == "BATTERY"
          "COMMAND" -> log.type == "COMMAND"
          "CARBON_COPY" -> log.type == "CARBON_COPY"
          else -> true
        }
        val matchesQuery = if (query.isBlank()) true else {
          log.title.contains(query, ignoreCase = true) ||
            log.content.contains(query, ignoreCase = true) ||
            log.extraInfo.contains(query, ignoreCase = true)
        }
        matchesFilter && matchesQuery
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    refreshDeviceStatus()
    checkServiceState()
  }

  fun setFilter(filter: String) {
    _selectedFilter.value = filter
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun refreshDeviceStatus() {
    val ctx = getApplication<Application>()
    _simCards.value = DeviceInfoHelper.getSimCards(ctx)
    _batteryInfo.value = DeviceInfoHelper.getBatteryInfo(ctx)
    _networkInfo.value = DeviceInfoHelper.getNetworkInfo(ctx)
  }

  fun checkServiceState() {
    _isServiceActive.value = TelegramSmsService.isRunning
  }

  fun toggleService(enable: Boolean) {
    val ctx = getApplication<Application>()
    viewModelScope.launch {
      val currentConfig = repository.getConfigDirect()
      repository.saveConfig(currentConfig.copy(isServiceRunning = enable))

      val intent = Intent(ctx, TelegramSmsService::class.java)
      if (enable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          ctx.startForegroundService(intent)
        } else {
          ctx.startService(intent)
        }
      } else {
        intent.action = TelegramSmsService.ACTION_STOP_SERVICE
        ctx.startService(intent)
      }
      _isServiceActive.value = enable
    }
  }

  fun updateConfig(updated: ForwardConfigEntity) {
    viewModelScope.launch {
      repository.saveConfig(updated)
    }
  }

  fun testTelegramBot(token: String, chatId: String, apiUrl: String) {
    _botTestState.value = BotTestUiState.Loading
    viewModelScope.launch {
      val result: TelegramTestResult = repository.testTelegramBot(token, chatId, apiUrl)
      if (result.success) {
        _botTestState.value = BotTestUiState.Success(
          botName = result.botName,
          username = result.username,
          latencyMs = result.latencyMs,
          targetChatCount = result.targetChatCount,
          deliveredChatCount = result.deliveredChatCount
        )
      } else {
        _botTestState.value = BotTestUiState.Error(result.errorMessage)
      }
    }
  }

  fun resetBotTestState() {
    _botTestState.value = BotTestUiState.Idle
  }

  fun resendLog(log: LogEntity) {
    viewModelScope.launch {
      repository.resendLog(log)
    }
  }

  fun deleteLog(id: Long) {
    viewModelScope.launch {
      repository.deleteLog(id)
    }
  }

  fun clearAllLogs() {
    viewModelScope.launch {
      repository.clearAllLogs()
    }
  }

  fun sendTestSmsForward(sender: String, message: String, simSlot: Int) {
    TelegramForwarder.forwardSms(
      context = getApplication(),
      sender = sender,
      content = message,
      simSlot = simSlot,
      timestamp = System.currentTimeMillis()
    )
  }

  fun sendTestCallForward(caller: String, simSlot: Int) {
    TelegramForwarder.forwardCall(
      context = getApplication(),
      caller = caller,
      simSlot = simSlot,
      timestamp = System.currentTimeMillis()
    )
  }

  fun sendTestBatteryForward(level: Int, isCharging: Boolean) {
    TelegramForwarder.forwardBattery(
      context = getApplication(),
      level = level,
      isCharging = isCharging,
      temperatureCelsius = 29.0f
    )
  }

  fun sendDirectSms(destination: String, text: String): String {
    return CommandProcessor.sendRealSms(getApplication(), destination, text)
  }

  fun executeConsoleCommand(command: String) {
    if (command.isBlank()) return
    val userMsg = ConsoleMessage("USER", command)
    val current = _consoleMessages.value.toMutableList()
    current.add(userMsg)
    _consoleMessages.value = current

    viewModelScope.launch {
      val result = CommandProcessor.processCommand(getApplication(), command)
      val botMsg = ConsoleMessage("BOT", result)
      val updated = _consoleMessages.value.toMutableList()
      updated.add(botMsg)
      _consoleMessages.value = updated
    }
  }

  fun exportConfigJson(): String {
    val cfg = config.value
    val json = JSONObject().apply {
      put("bot_token", cfg.botToken)
      put("chat_id", cfg.chatId)
      put("trusted_chat_id", cfg.trustedChatId)
      put("custom_api_url", cfg.customApiUrl)
      put("sms_forward", cfg.isSmsForwardEnabled)
      put("call_forward", cfg.isCallForwardEnabled)
      put("battery_forward", cfg.isBatteryForwardEnabled)
      put("battery_threshold", cfg.batteryThreshold)
      put("charging_alert", cfg.isChargingAlertEnabled)
      put("carbon_copy", cfg.isCarbonCopyEnabled)
      put("bark_url", cfg.barkUrl)
      put("pushdeer_key", cfg.pushDeerKey)
      put("gotify_url", cfg.gotifyUrl)
      put("gotify_token", cfg.gotifyToken)
      put("custom_webhook", cfg.customWebhookUrl)
    }
    return json.toString(2)
  }

  fun importConfigJson(jsonStr: String): Boolean {
    return try {
      val json = JSONObject(jsonStr)
      val current = config.value
      val updated = current.copy(
        botToken = json.optString("bot_token", current.botToken),
        chatId = json.optString("chat_id", current.chatId),
        trustedChatId = json.optString("trusted_chat_id", current.trustedChatId),
        customApiUrl = json.optString("custom_api_url", current.customApiUrl),
        isSmsForwardEnabled = json.optBoolean("sms_forward", current.isSmsForwardEnabled),
        isCallForwardEnabled = json.optBoolean("call_forward", current.isCallForwardEnabled),
        isBatteryForwardEnabled = json.optBoolean("battery_forward", current.isBatteryForwardEnabled),
        batteryThreshold = json.optInt("battery_threshold", current.batteryThreshold),
        isChargingAlertEnabled = json.optBoolean("charging_alert", current.isChargingAlertEnabled),
        isCarbonCopyEnabled = json.optBoolean("carbon_copy", current.isCarbonCopyEnabled),
        barkUrl = json.optString("bark_url", current.barkUrl),
        pushDeerKey = json.optString("pushdeer_key", current.pushDeerKey),
        gotifyUrl = json.optString("gotify_url", current.gotifyUrl),
        gotifyToken = json.optString("gotify_token", current.gotifyToken),
        customWebhookUrl = json.optString("custom_webhook", current.customWebhookUrl)
      )
      updateConfig(updated)
      true
    } catch (e: Exception) {
      false
    }
  }
}
