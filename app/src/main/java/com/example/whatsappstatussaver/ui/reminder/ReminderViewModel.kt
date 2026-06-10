package com.example.whatsappstatussaver.ui.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatsappstatussaver.data.local.entity.ReminderEntity
import com.example.whatsappstatussaver.data.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val repository: ReminderRepository
) : ViewModel() {

    val allReminders: StateFlow<List<ReminderEntity>> = repository.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.addReminder(reminder)
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }
}
