package com.example.nalla_nudi.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words ORDER BY english_term ASC")
    fun getAllWords(): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE english_term LIKE '%' || :query || '%' ORDER BY english_term ASC")
    fun searchWords(query: String): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE subject_category = :category ORDER BY english_term ASC")
    fun getWordsByCategory(category: String): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE is_saved = 1")
    fun getSavedWords(): Flow<List<Word>>

    @Query("SELECT COUNT(*) FROM words WHERE is_learned = 1")
    fun getLearnedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM words")
    fun getTotalCount(): Flow<Int>

    @Update
    suspend fun updateWord(word: Word)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<Word>)

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun getWordById(id: Int): Word?

    @Query("SELECT * FROM words ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomWord(): Word?
}
