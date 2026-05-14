package com.example.nalla_nudi.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nalla_nudi.data.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NallaNudiViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WordRepository
    private val userPreferences: UserPreferences = UserPreferences(application)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = WordRepository(database.wordDao())
        preloadData()
        viewModelScope.launch {
            userPreferences.userName.filterNotNull().collect { name ->
                if (name.isNotBlank()) updateWordOfDay(name)
            }
        }
    }

    val isLoggedIn = userPreferences.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val userName = userPreferences.userName
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val totalCount = repository.totalCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val userWords: StateFlow<List<Word>> = combine(
        repository.allWords,
        userPreferences.savedWordIds,
        userPreferences.learnedWordIds
    ) { allWords, savedIds, learnedIds ->
        allWords.map { word ->
            word.copy(
                is_saved = savedIds.contains(word.id),
                is_learned = learnedIds.contains(word.id)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val learnedCount: StateFlow<Int> = userPreferences.learnedWordIds
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    val filteredWords: StateFlow<List<Word>> = combine(
        _searchQuery,
        _selectedCategory,
        userWords
    ) { query, category, words ->
        val cleanQuery = query.trim()
        words.filter { word ->
            (category == "All" || word.subject_category == category) &&
                    (cleanQuery.isBlank() ||
                            word.english_term.contains(cleanQuery, ignoreCase = true) ||
                            word.kannada_meaning.contains(cleanQuery, ignoreCase = true) ||
                            word.kannada_explanation.contains(cleanQuery, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedWords = userWords.map { words -> words.filter { it.is_saved } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _wordOfDay = MutableStateFlow<Word?>(null)
    val wordOfDay = _wordOfDay.asStateFlow()

    private val _authMessage = MutableStateFlow<String?>(null)
    val authMessage = _authMessage.asStateFlow()

    private fun preloadData() {
        viewModelScope.launch {
            val count = repository.totalCount.first()
            if (count == 0) {
                try {
                    val jsonString = getApplication<Application>()
                        .assets.open("words.json")
                        .bufferedReader()
                        .use { it.readText() }

                    val listType = object : TypeToken<List<Word>>() {}.type
                    val words: List<Word> = Gson().fromJson(jsonString, listType)

                    repository.insertAll(words)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun login(name: String) {
        viewModelScope.launch {
            _authMessage.value = when (userPreferences.login(name)) {
                AuthResult.Success -> null
                AuthResult.EmptyName -> "Enter your name."
                AuthResult.UserAlreadyExists -> null
                AuthResult.UserNotFound -> "No user exists with this name. Register first."
            }
        }
    }

    fun register(name: String) {
        viewModelScope.launch {
            _authMessage.value = when (userPreferences.register(name)) {
                AuthResult.Success -> null
                AuthResult.EmptyName -> "Enter your name."
                AuthResult.UserAlreadyExists -> "This name is already registered. Login instead."
                AuthResult.UserNotFound -> null
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userPreferences.logout()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun toggleSave(word: Word) {
        viewModelScope.launch {
            userPreferences.setWordSaved(word.id, !word.is_saved)
        }
    }

    fun markAsLearned(word: Word) {
        if (!word.is_learned) {
            viewModelScope.launch {
                userPreferences.markWordLearned(word.id)
            }
        }
    }

    private suspend fun updateWordOfDay(name: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        userPreferences.lastWordOfDayInfo(name).let { (id, date) ->
            if (date == today && id != null) {
                _wordOfDay.value = repository.getWordById(id)
            } else {
                val randomWord = repository.getRandomWord()
                randomWord?.let {
                    userPreferences.saveWordOfDay(it.id, today, name)
                    _wordOfDay.value = it
                }
            }
        }
    }
}
