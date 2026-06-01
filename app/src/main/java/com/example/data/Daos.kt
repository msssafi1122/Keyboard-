package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ClipboardItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClipboardItem)

    @Query("DELETE FROM clipboard_history WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM clipboard_history")
    suspend fun clearAll()
}

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM user_dictionary ORDER BY frequency DESC")
    fun getAll(): Flow<List<DictionaryEntry>>

    @Query("SELECT * FROM user_dictionary WHERE word LIKE :query || '%' ORDER BY frequency DESC LIMIT 5")
    suspend fun getSuggestions(query: String): List<DictionaryEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DictionaryEntry)

    @Query("DELETE FROM user_dictionary WHERE word = :word")
    suspend fun delete(word: String)
}

@Dao
interface FrequentlyUsedDao {
    @Query("SELECT * FROM frequently_used ORDER BY usageCount DESC, id DESC")
    fun getAll(): Flow<List<FrequentlyUsed>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FrequentlyUsed)

    @Query("DELETE FROM frequently_used WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("UPDATE frequently_used SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsage(id: Int)
}
