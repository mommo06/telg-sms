package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "forward_logs")
data class LogEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val type: String, // "SMS", "CALL", "BATTERY", "COMMAND", "CARBON_COPY", "SYSTEM"
  val title: String,
  val content: String,
  val timestamp: Long = System.currentTimeMillis(),
  val simSlot: Int = -1, // 0 = SIM 1, 1 = SIM 2, -1 = N/A
  val status: String = "SUCCESS", // "SUCCESS", "FAILED", "PENDING", "RETRIED"
  val extraInfo: String = ""
)
