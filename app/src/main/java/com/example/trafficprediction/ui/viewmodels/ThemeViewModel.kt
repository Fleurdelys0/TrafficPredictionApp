package com.example.trafficprediction.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trafficprediction.data.ThemePreference
import com.example.trafficprediction.data.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)

    val themePreference: StateFlow<ThemePreference> = userPreferencesRepository.themePreferenceFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemePreference.SYSTEM_DEFAULT // Initial value.
        )

    fun updateThemePreference(newThemePreference: ThemePreference) {
        viewModelScope.launch {
            userPreferencesRepository.updateThemePreference(newThemePreference)
        }
    }
}
