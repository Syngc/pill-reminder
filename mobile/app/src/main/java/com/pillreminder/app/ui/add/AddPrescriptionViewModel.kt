package com.pillreminder.app.ui.add

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pillreminder.app.PillReminderApp
import com.pillreminder.app.R
import com.pillreminder.app.data.Medication
import com.pillreminder.app.network.ExtractedMedication
import com.pillreminder.app.network.ExtractionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

/** An editable medicine on the review screen. */
data class DraftMedication(
    val key: Long,
    val name: String = "",
    val dose: String = "",
    val times: List<String> = emptyList(),
    val durationDays: String = "",
    val instructions: String = "",
    val needsCheck: Boolean = false,
    val notes: String = "",
    val timesSuggested: Boolean = false,
) {
    val isComplete get() = name.isNotBlank() && dose.isNotBlank() && times.isNotEmpty()
}

sealed interface AddState {
    data object Choose : AddState
    data object Processing : AddState
    /** [message] is shown when present (already in the app language); otherwise [fallback]. */
    data class Failed(val message: String? = null, @StringRes val fallback: Int = R.string.add_unreadable) : AddState
    data class Review(
        val drafts: List<DraftMedication>,
        val warnings: List<String>,
        val confirmed: Boolean = false,
        val showMissing: Boolean = false,
    ) : AddState
    data object Saved : AddState
}

class AddPrescriptionViewModel(private val app: PillReminderApp) : ViewModel() {
    private val _state = MutableStateFlow<AddState>(AddState.Choose)
    val state: StateFlow<AddState> = _state.asStateFlow()
    private val keys = AtomicLong()

    fun extract(photo: Uri) {
        _state.value = AddState.Processing
        viewModelScope.launch {
            _state.value = try {
                val jpeg = withContext(Dispatchers.Default) { ImageCompressor.toJpeg(app, photo) }
                val result = app.extractionApi.extract(jpeg, app.language)
                if (!result.readable || result.medications.isEmpty()) {
                    AddState.Failed(result.warnings.firstOrNull())
                } else {
                    AddState.Review(result.medications.map { it.toDraft() }, result.warnings)
                }
            } catch (e: ExtractionException.NoConnection) {
                AddState.Failed(fallback = R.string.error_no_connection)
            } catch (e: ExtractionException.BadResponse) {
                AddState.Failed(fallback = R.string.error_bad_response)
            } catch (e: ExtractionException.Server) {
                AddState.Failed(e.detail)
            } catch (e: Exception) {
                AddState.Failed()
            }
        }
    }

    fun startManual() {
        _state.value = AddState.Review(listOf(DraftMedication(keys.incrementAndGet())), emptyList())
    }

    fun reset() {
        _state.value = AddState.Choose
    }

    fun updateDraft(draft: DraftMedication) = editReview { review ->
        review.copy(drafts = review.drafts.map { if (it.key == draft.key) draft else it })
    }

    fun addDraft() = editReview { it.copy(drafts = it.drafts + DraftMedication(keys.incrementAndGet())) }

    fun removeDraft(key: Long) = editReview { review -> review.copy(drafts = review.drafts.filterNot { it.key == key }) }

    fun setConfirmed(confirmed: Boolean) = editReview { it.copy(confirmed = confirmed) }

    fun save() {
        val review = _state.value as? AddState.Review ?: return
        if (review.drafts.isEmpty() || !review.drafts.all { it.isComplete }) {
            _state.value = review.copy(showMissing = true)
            return
        }
        if (!review.confirmed) return
        viewModelScope.launch {
            val now = LocalDateTime.now()
            app.repository.add(
                review.drafts.map {
                    Medication(
                        name = it.name.trim(),
                        dose = it.dose.trim(),
                        times = it.times.distinct().sorted(),
                        instructions = it.instructions.trim(),
                        startDate = now.toLocalDate(),
                        durationDays = it.durationDays.toIntOrNull()?.takeIf { days -> days > 0 },
                        scheduleSince = now,
                    )
                }
            )
            app.scheduler.rescheduleAll()
            _state.value = AddState.Saved
        }
    }

    private fun editReview(change: (AddState.Review) -> AddState.Review) {
        _state.update { if (it is AddState.Review) change(it) else it }
    }

    private fun ExtractedMedication.toDraft() = DraftMedication(
        key = keys.incrementAndGet(),
        name = name,
        dose = dose,
        times = times.filter { TIME_PATTERN.matches(it) }.distinct().sorted(),
        durationDays = durationDays?.toString().orEmpty(),
        instructions = instructions,
        needsCheck = confidence != "high" || notesForReviewer.isNotBlank(),
        notes = notesForReviewer,
        timesSuggested = timesAreSuggested,
    )

    companion object {
        private val TIME_PATTERN = Regex("""^([01]\d|2[0-3]):[0-5]\d$""")

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AddPrescriptionViewModel(this[APPLICATION_KEY] as PillReminderApp) }
        }
    }
}
