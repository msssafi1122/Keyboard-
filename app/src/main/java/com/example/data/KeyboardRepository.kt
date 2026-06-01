package com.example.data

import kotlinx.coroutines.flow.Flow

class KeyboardRepository(private val database: AppDatabase) {
    val clipboardDao = database.clipboardDao()
    val dictionaryDao = database.dictionaryDao()
    val frequentlyUsedDao = database.frequentlyUsedDao()

    val allClipboardItems: Flow<List<ClipboardItem>> = clipboardDao.getAll()
    val allDictionary: Flow<List<DictionaryEntry>> = dictionaryDao.getAll()
    val allFrequentlyUsed: Flow<List<FrequentlyUsed>> = frequentlyUsedDao.getAll()

    suspend fun getSuggestions(query: String): List<DictionaryEntry> {
        return dictionaryDao.getSuggestions(query)
    }

    suspend fun insertClipboard(item: ClipboardItem) {
        clipboardDao.insert(item)
    }

    suspend fun deleteClipboard(id: Int) {
        clipboardDao.delete(id)
    }

    suspend fun clearClipboard() {
        clipboardDao.clearAll()
    }

    suspend fun insertWord(entry: DictionaryEntry) {
        dictionaryDao.insert(entry)
    }

    suspend fun deleteWord(word: String) {
        dictionaryDao.delete(word)
    }

    suspend fun insertFrequentlyUsed(item: FrequentlyUsed) {
        frequentlyUsedDao.insert(item)
    }

    suspend fun deleteFrequentlyUsed(id: Int) {
        frequentlyUsedDao.delete(id)
    }

    suspend fun incrementPhraseUsage(id: Int) {
        frequentlyUsedDao.incrementUsage(id)
    }
}
