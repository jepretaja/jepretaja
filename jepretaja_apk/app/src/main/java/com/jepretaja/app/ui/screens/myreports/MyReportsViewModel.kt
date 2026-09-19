package com.jepretaja.app.ui.screens.myreports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jepretaja.app.data.model.ReportModel
import com.jepretaja.app.data.repository.ExploreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyReportsViewModel @Inject constructor(
    private val repository: ExploreRepository,
) : ViewModel() {
    private val _submitting = MutableStateFlow(false)
    val submitting = _submitting.asStateFlow()

    fun myReports(userId: String): Flow<List<ReportModel>> = repository.streamMyReports(userId)

    fun submit(userId: String, targetType: String, targetId: String, reason: String) {
        viewModelScope.launch {
            _submitting.value = true
            runCatching {
                if (targetType == "creator") repository.reportCreator(targetId, userId, reason)
                else repository.report(targetId, userId, reason)
            }
            _submitting.value = false
        }
    }
}
