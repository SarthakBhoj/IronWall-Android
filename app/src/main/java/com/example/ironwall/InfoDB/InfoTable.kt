package com.example.ironwall.InfoDB

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "users")
data class UserTable(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val userId: String,
    val pin : String
)
