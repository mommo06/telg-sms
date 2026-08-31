package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.example.service.TelegramForwarder

class CallReceiver : BroadcastReceiver() {

  companion object {
    private var lastState = TelephonyManager.EXTRA_STATE_IDLE
    private var incomingNumber: String? = null
    private var isMissed = false
  }

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

    val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
    val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

    if (!number.isNullOrBlank()) {
      incomingNumber = number
    }

    val subId = intent.getIntExtra("subscription", -1)
    val simSlot = if (subId >= 0) subId % 2 else 0

    when (state) {
      TelephonyManager.EXTRA_STATE_RINGING -> {
        isMissed = true
        lastState = TelephonyManager.EXTRA_STATE_RINGING
      }
      TelephonyManager.EXTRA_STATE_OFFHOOK -> {
        isMissed = false
        lastState = TelephonyManager.EXTRA_STATE_OFFHOOK
      }
      TelephonyManager.EXTRA_STATE_IDLE -> {
        if (lastState == TelephonyManager.EXTRA_STATE_RINGING && isMissed) {
          val caller = incomingNumber ?: "Private / Unknown"
          TelegramForwarder.forwardCall(context, caller, simSlot, System.currentTimeMillis())
        }
        lastState = TelephonyManager.EXTRA_STATE_IDLE
        incomingNumber = null
        isMissed = false
      }
    }
  }
}
