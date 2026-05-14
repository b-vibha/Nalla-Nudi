package com.example.nalla_nudi.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        val USER_NAME = stringPreferencesKey("user_name")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val USERS = stringSetPreferencesKey("registered_users")

        private fun userKey(name: String): String {
            return name.trim()
                .lowercase(Locale.ROOT)
                .replace(Regex("[^a-z0-9_]"), "_")
        }

        private fun savedWordsKey(name: String) = stringSetPreferencesKey("saved_words_${userKey(name)}")
        private fun learnedWordsKey(name: String) = stringSetPreferencesKey("learned_words_${userKey(name)}")
        private fun lastWordOfDayIdKey(name: String) = intPreferencesKey("last_word_id_${userKey(name)}")
        private fun lastDateKey(name: String) = stringPreferencesKey("last_date_${userKey(name)}")

        private fun storedName(userEntry: String) = userEntry.substringBefore(":").trim()
    }

    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[IS_LOGGED_IN] ?: false }

    suspend fun register(name: String): AuthResult {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return AuthResult.EmptyName

        var result: AuthResult = AuthResult.Success
        context.dataStore.edit { preferences ->
            val users = preferences[USERS].orEmpty()
            val existingUser = users.find { storedName(it).equals(cleanName, ignoreCase = true) }
            if (existingUser != null) {
                result = AuthResult.UserAlreadyExists
            } else {
                preferences[USERS] = users + cleanName
                preferences[USER_NAME] = cleanName
                preferences[IS_LOGGED_IN] = true
            }
        }
        return result
    }

    suspend fun login(name: String): AuthResult {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return AuthResult.EmptyName

        var result: AuthResult = AuthResult.UserNotFound
        context.dataStore.edit { preferences ->
            val users = preferences[USERS].orEmpty()
            val existingUser = users.find { storedName(it).equals(cleanName, ignoreCase = true) }
            val savedName = existingUser?.let { storedName(it) }

            if (savedName != null) {
                preferences[USERS] = users - existingUser + savedName
                preferences[USER_NAME] = savedName
                preferences[IS_LOGGED_IN] = true
                result = AuthResult.Success
            }
        }
        return result
    }

    suspend fun logout() {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = false
            preferences.remove(USER_NAME)
        }
    }

    val savedWordIds: Flow<Set<Int>> = context.dataStore.data.map { preferences ->
        val name = preferences[USER_NAME].orEmpty()
        preferences[savedWordsKey(name)].orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
    }

    val learnedWordIds: Flow<Set<Int>> = context.dataStore.data.map { preferences ->
        val name = preferences[USER_NAME].orEmpty()
        preferences[learnedWordsKey(name)].orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
    }

    suspend fun setWordSaved(wordId: Int, isSaved: Boolean) {
        context.dataStore.edit { preferences ->
            val name = preferences[USER_NAME].orEmpty()
            if (name.isBlank()) return@edit
            val key = savedWordsKey(name)
            val ids = preferences[key].orEmpty()
            preferences[key] = if (isSaved) ids + wordId.toString() else ids - wordId.toString()
        }
    }

    suspend fun markWordLearned(wordId: Int) {
        context.dataStore.edit { preferences ->
            val name = preferences[USER_NAME].orEmpty()
            if (name.isBlank()) return@edit
            val key = learnedWordsKey(name)
            preferences[key] = preferences[key].orEmpty() + wordId.toString()
        }
    }

    suspend fun saveWordOfDay(id: Int, date: String, name: String) {
        context.dataStore.edit { preferences ->
            preferences[lastWordOfDayIdKey(name)] = id
            preferences[lastDateKey(name)] = date
        }
    }

    suspend fun lastWordOfDayInfo(name: String): Pair<Int?, String?> {
        val preferences = context.dataStore.data.first()
        return Pair(preferences[lastWordOfDayIdKey(name)], preferences[lastDateKey(name)])
    }
}

enum class AuthResult {
    Success,
    EmptyName,
    UserAlreadyExists,
    UserNotFound
}
