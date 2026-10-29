package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PendingQueueEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Room offline queue management.
 */
@Dao
interface PendingQueueDao {

    @Query("SELECT * FROM pending_queue ORDER BY timestamp ASC")
    fun getAllPendingFlow(): Flow<List<PendingQueueEntity>>

    @Query("SELECT * FROM pending_queue ORDER BY timestamp ASC")
    suspend fun getAllPendingList(): List<PendingQueueEntity>

    @Query("SELECT * FROM pending_queue WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PendingQueueEntity?

    @Query("SELECT COUNT(*) FROM pending_queue")
    fun getPendingCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: PendingQueueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<PendingQueueEntity>)

    @Update
    suspend fun update(task: PendingQueueEntity)

    @Query("UPDATE pending_queue SET status = :status, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM pending_queue WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM pending_queue")
    suspend fun clearAll()
}
