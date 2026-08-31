package com.example.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager

data class SimCardInfo(
  val slotIndex: Int,
  val displayName: String,
  val carrierName: String,
  val countryIso: String,
  val subscriptionId: Int
)

data class BatteryInfo(
  val level: Int,
  val isCharging: Boolean,
  val temperatureCelsius: Float,
  val health: String,
  val pluggedType: String
)

data class DeviceNetworkInfo(
  val isConnected: Boolean,
  val networkType: String // "Wi-Fi", "Cellular (5G/LTE)", "Ethernet", "None"
)

object DeviceInfoHelper {

  @SuppressLint("MissingPermission")
  fun getSimCards(context: Context): List<SimCardInfo> {
    val simList = mutableListOf<SimCardInfo>()
    try {
      val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
      if (subscriptionManager != null) {
        val activeSubscriptions: List<SubscriptionInfo>? = subscriptionManager.activeSubscriptionInfoList
        if (!activeSubscriptions.isNullOrEmpty()) {
          for (sub in activeSubscriptions) {
            simList.add(
              SimCardInfo(
                slotIndex = sub.simSlotIndex,
                displayName = sub.displayName?.toString() ?: "SIM ${sub.simSlotIndex + 1}",
                carrierName = sub.carrierName?.toString() ?: "Unknown Carrier",
                countryIso = sub.countryIso?.uppercase() ?: "--",
                subscriptionId = sub.subscriptionId
              )
            )
          }
        }
      }
    } catch (e: SecurityException) {
      // Permission not granted yet
    } catch (e: Exception) {
      // Fallback
    }

    if (simList.isEmpty()) {
      // Default fallback representation
      val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
      val simOperator = telephony?.networkOperatorName.takeIf { !it.isNullOrBlank() } ?: "SIM Card"
      simList.add(
        SimCardInfo(
          slotIndex = 0,
          displayName = "SIM 1",
          carrierName = simOperator,
          countryIso = telephony?.networkCountryIso?.uppercase() ?: "US",
          subscriptionId = 1
        )
      )
    }

    return simList
  }

  fun getBatteryInfo(context: Context): BatteryInfo {
    val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    val batteryStatus: Intent? = context.registerReceiver(null, filter)

    val level: Int = batteryStatus?.let { intent ->
      val rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
      val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
      if (rawLevel >= 0 && scale > 0) (rawLevel * 100 / scale) else 80
    } ?: 85

    val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
      status == BatteryManager.BATTERY_STATUS_FULL

    val tempRaw = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280
    val temperature = tempRaw / 10f

    val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
    val pluggedType = when (chargePlug) {
      BatteryManager.BATTERY_PLUGGED_AC -> "AC Wall Power"
      BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable"
      BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Dock"
      else -> if (isCharging) "Charging" else "Discharging"
    }

    val healthRaw = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD)
    val health = when (healthRaw) {
      BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
      BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
      BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
      BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
      else -> "Normal"
    }

    return BatteryInfo(
      level = level,
      isCharging = isCharging,
      temperatureCelsius = temperature,
      health = health,
      pluggedType = pluggedType
    )
  }

  fun getNetworkInfo(context: Context): DeviceNetworkInfo {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      ?: return DeviceNetworkInfo(false, "None")

    val network = cm.activeNetwork ?: return DeviceNetworkInfo(false, "Disconnected")
    val caps = cm.getNetworkCapabilities(network) ?: return DeviceNetworkInfo(false, "Disconnected")

    val type = when {
      caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
      caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular (5G / 4G LTE)"
      caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
      caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth Tether"
      else -> "Connected"
    }

    return DeviceNetworkInfo(true, type)
  }
}
