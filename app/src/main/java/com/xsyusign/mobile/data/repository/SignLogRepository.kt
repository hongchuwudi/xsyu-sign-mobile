package com.xsyusign.mobile.data.repository

import com.xsyusign.mobile.data.dao.SignLogDao
import com.xsyusign.mobile.data.entity.SignLog
import kotlinx.coroutines.flow.Flow

class SignLogRepository(private val dao: SignLogDao) {

    fun observeRecent(limit: Int = 50): Flow<List<SignLog>> = dao.observeRecent(limit)

    fun observeByUser(userId: Long, limit: Int = 30): Flow<List<SignLog>> = dao.observeByUser(userId, limit)

    suspend fun insert(log: SignLog) = dao.insert(log)

    suspend fun cleanOldLogs() {
        dao.deleteOlderThan(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)
    }

    suspend fun cleanAllLogs() = dao.deleteAll()
}
