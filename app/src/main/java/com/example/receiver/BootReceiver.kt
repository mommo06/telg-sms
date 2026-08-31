package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.repository.TelegramSmsRepository
import com.example.service.TelegramSmsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

    CoroutineScope(Dispatchers.IO).launch {
      val db = AppDatabase.getDatabase(context)
      val repository = TelegramSmsRepository(db.telegramSmsDao())
      val config = repository.getConfigDirect()

      if (config.isServiceRunning) {
        val serviceIntent = Intent(context, TelegramSmsService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(serviceIntent)
        } else {
          context.startService(serviceIntent)
        }
      }
    }
  }
}
