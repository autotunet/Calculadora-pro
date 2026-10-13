package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val path: String,
    val name: String,
    val isDirectory: Boolean,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recents")
data class RecentEntity(
    @PrimaryKey val path: String,
    val name: String,
    val isDirectory: Boolean,
    val lastOpenedAt: Long = System.currentTimeMillis()
)
