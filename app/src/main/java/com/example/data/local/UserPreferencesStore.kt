package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.SavedRepoSummary
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gitPulseDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "gitpulse_preferences"
)

data class UserPreferences(
    val darkMode: Boolean = true,
    val connectedUsername: String = "",
    val githubToken: String = "",
    val followedUsers: List<String> = DEFAULT_FOLLOWED_USERS,
    val savedRepos: List<SavedRepoSummary> = emptyList()
) {
    companion object {
        val DEFAULT_FOLLOWED_USERS = listOf(
            "antfu",
            "shadcn",
            "JakeWharton",
            "tiangolo",
            "sindresorhus",
            "torvalds",
            "google",
            "JetBrains"
        )
    }
}

class UserPreferencesStore(
    private val context: Context,
    moshi: Moshi
) {
    private val savedRepoListType = Types.newParameterizedType(
        List::class.java,
        SavedRepoSummary::class.java
    )
    private val savedRepoAdapter = moshi.adapter<List<SavedRepoSummary>>(savedRepoListType)

    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val CONNECTED_USERNAME = stringPreferencesKey("connected_username")
        val GITHUB_TOKEN = stringPreferencesKey("github_token")
        val FOLLOWED_USERS = stringSetPreferencesKey("followed_users")
        val SAVED_REPOS_JSON = stringPreferencesKey("saved_repos_json")
    }

    val preferencesFlow: Flow<UserPreferences> = context.gitPulseDataStore.data.map { prefs ->
        val savedJson = prefs[Keys.SAVED_REPOS_JSON].orEmpty()
        val savedList = if (savedJson.isBlank()) {
            emptyList()
        } else {
            runCatching { savedRepoAdapter.fromJson(savedJson).orEmpty() }.getOrDefault(emptyList())
        }
        val followed = prefs[Keys.FOLLOWED_USERS]?.toList()?.sortedBy { it.lowercase() }
            ?: UserPreferences.DEFAULT_FOLLOWED_USERS
        UserPreferences(
            darkMode = prefs[Keys.DARK_MODE] ?: true,
            connectedUsername = prefs[Keys.CONNECTED_USERNAME].orEmpty(),
            githubToken = prefs[Keys.GITHUB_TOKEN].orEmpty(),
            followedUsers = followed,
            savedRepos = savedList.sortedByDescending { it.savedAtMillis }
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.gitPulseDataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun updateConnection(username: String, token: String) {
        context.gitPulseDataStore.edit {
            it[Keys.CONNECTED_USERNAME] = username.trim()
            it[Keys.GITHUB_TOKEN] = token.trim()
        }
    }

    suspend fun toggleFollowUser(login: String) {
        val normalized = login.trim()
        if (normalized.isBlank()) return
        context.gitPulseDataStore.edit { prefs ->
            val current = (prefs[Keys.FOLLOWED_USERS] ?: UserPreferences.DEFAULT_FOLLOWED_USERS.toSet())
                .toMutableSet()
            val existing = current.firstOrNull { it.equals(normalized, ignoreCase = true) }
            if (existing != null) {
                current.remove(existing)
            } else {
                current.add(normalized)
            }
            prefs[Keys.FOLLOWED_USERS] = current
        }
    }

    suspend fun toggleSavedRepo(summary: SavedRepoSummary) {
        context.gitPulseDataStore.edit { prefs ->
            val currentJson = prefs[Keys.SAVED_REPOS_JSON].orEmpty()
            val currentList = if (currentJson.isBlank()) {
                mutableListOf()
            } else {
                runCatching { savedRepoAdapter.fromJson(currentJson).orEmpty().toMutableList() }
                    .getOrDefault(mutableListOf())
            }
            val existingIndex = currentList.indexOfFirst {
                it.fullName.equals(summary.fullName, ignoreCase = true)
            }
            if (existingIndex >= 0) {
                currentList.removeAt(existingIndex)
            } else {
                currentList.add(0, summary.copy(savedAtMillis = System.currentTimeMillis()))
            }
            prefs[Keys.SAVED_REPOS_JSON] = savedRepoAdapter.toJson(currentList)
        }
    }
}
