package com.example.nalla_nudi.data

import kotlinx.coroutines.flow.Flow

class WordRepository(private val wordDao: WordDao) {
    val allWords: Flow<List<Word>> = wordDao.getAllWords()
    val savedWords: Flow<List<Word>> = wordDao.getSavedWords()
    val learnedCount: Flow<Int> = wordDao.getLearnedCount()
    val totalCount: Flow<Int> = wordDao.getTotalCount()

    fun searchWords(query: String): Flow<List<Word>> = wordDao.searchWords(query)

    fun getWordsByCategory(category: String): Flow<List<Word>> = wordDao.getWordsByCategory(category)

    suspend fun updateWord(word: Word) {
        wordDao.updateWord(word)
    }

    suspend fun insertAll(words: List<Word>) {
        wordDao.insertAll(words)
    }

    suspend fun getWordById(id: Int): Word? = wordDao.getWordById(id)

    suspend fun getRandomWord(): Word? = wordDao.getRandomWord()
}
