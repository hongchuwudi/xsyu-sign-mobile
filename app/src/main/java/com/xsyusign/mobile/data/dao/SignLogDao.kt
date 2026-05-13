package com.xsyusign.mobile.data.dao

import androidx.room.*
import com.xsyusign.mobile.data.entity.SignLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SignLogDao {

    @Query("SELECT * FROM sign_logs ORDER BY created_at DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<SignLog>>

    @Query("SELECT * FROM sign_logs WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit")
    fun observeByUser(userId: Long, limit: Int = 30): Flow<List<SignLog>>

    @Insert
    suspend fun insert(log: SignLog): Long

    @Query("DELETE FROM sign_logs WHERE created_at < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM sign_logs")
    suspend fun deleteAll()
}
