package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clipboard_history")
data class ClipboardItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_dictionary")
data class DictionaryEntry(
    @PrimaryKey val word: String,
    val language: String, // "bn" or "en"
    val frequency: Int = 1
)

@Entity(tableName = "frequently_used")
data class FrequentlyUsed(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val shortcut: String = "",
    val usageCount: Int = 0
)
