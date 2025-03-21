package ru.samolet.indoorinspection.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NavigationViewModel : ViewModel() {
    private val mutableParameters = MutableStateFlow<InspectionParameters>(NoInspection)
    val selectedParameters: StateFlow<InspectionParameters> get() = mutableParameters

    fun updateInspection(currentInspection: InspectionParameters) {
        mutableParameters.value = currentInspection
    }
}