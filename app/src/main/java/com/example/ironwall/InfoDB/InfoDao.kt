package com.example.ironwall.InfoDB

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserTable)

    @Update
    suspend fun update(user: UserTable)

    @Delete
    suspend fun delete(user: UserTable)

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserTable?

    @Query("SELECT * FROM users ORDER BY username ASC")
    fun getAllUsers(): Flow<List<UserTable>>

    @Query("SELECT * FROM users ORDER BY id DESC LIMIT 1")
    fun getLastUser(): Flow<UserTable?>



}
