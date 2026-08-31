package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ForwardConfigEntity
import com.example.data.model.LogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TelegramSmsDao {

  @Query("SELECT * FROM forward_logs ORDER BY timestamp DESC")
  fun getAllLogs(): Flow<List<LogEntity>>

  @Query("SELECT * FROM forward_logs WHERE type = :type ORDER BY timestamp DESC")
  fun getLogsByType(type: String): Flow<List<LogEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLog(log: LogEntity): Long

  @Update
  suspend fun updateLog(log: LogEntity)

  @Query("DELETE FROM forward_logs WHERE id = :id")
  suspend fun deleteLogById(id: Long)

  @Query("DELETE FROM forward_logs")
  suspend fun clearAllLogs()

  @Query("SELECT * FROM forward_config WHERE id = 1")
  fun getConfigFlow(): Flow<ForwardConfigEntity?>

  @Query("SELECT * FROM forward_config WHERE id = 1")
  suspend fun getConfigDirect(): ForwardConfigEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun saveConfig(config: ForwardConfigEntity)
}
