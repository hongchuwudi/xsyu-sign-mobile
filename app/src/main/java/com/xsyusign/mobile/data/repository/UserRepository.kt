package com.xsyusign.mobile.data.repository

import com.xsyusign.mobile.data.dao.UserDao
import com.xsyusign.mobile.data.entity.User
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {

    fun observeAll(): Flow<List<User>> = userDao.observeAll()

    suspend fun getAll(): List<User> = userDao.getAll()

    suspend fun getById(id: Long): User? = userDao.getById(id)

    suspend fun getByUsername(username: String): User? = userDao.getByUsername(username)

    suspend fun getAutoSignUsers(): List<User> = userDao.getAutoSignUsers()

    suspend fun save(user: User): Long = userDao.upsert(user)

    suspend fun update(user: User) = userDao.update(user)

    suspend fun delete(user: User) = userDao.delete(user)

    suspend fun deleteById(id: Long) = userDao.deleteById(id)
}
