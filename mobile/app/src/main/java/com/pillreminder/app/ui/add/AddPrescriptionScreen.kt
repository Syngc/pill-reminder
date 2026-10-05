package com.pillreminder.app.ui.add

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pillreminder.app.R
import com.pillreminder.app.ui.TimesEditor
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPrescriptionScreen(
    onDone: () -> Unit,
    viewModel: AddPrescriptionViewModel = viewModel(factory = AddPrescriptionViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state) { if (state is AddState.Saved) onDone() }

    val title = if (state is AddState.Review) R.string.review_title else R.string.add_title
    val goBack = { if (state is AddState.Review || state is AddState.Failed) viewModel.reset() else onDone() }
    BackHandler(onBack = goBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = {
                    IconButton(onClick = goBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                AddState.Choose -> ChooseSource(onPhoto = viewModel::extract, onManual = viewModel::startManual)
                AddState.Processing -> Processing()
                is AddState.Failed -> Failed(
                    s.message ?: stringResource(s.fallback),
                    onRetry = viewModel::reset,
                    onManual = viewModel::startManual,
                )
                is AddState.Review -> Review(s, viewModel)
                AddState.Saved -> Unit
            }
        }
    }
}

@Composable
private fun ChooseSource(onPhoto: (Uri) -> Unit, onManual: () -> Unit) {
    val context = LocalContext.current
    var pendingPhoto by rememberSaveable { mutableStateOf<Uri?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingPhoto
        if (saved && uri != null) onPhoto(uri)
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPhoto(uri)
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.add_intro), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val dir = File(context.cacheDir, "photos").apply { mkdirs() }
                val file = File(dir, "receta-${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                pendingPhoto = uri
                camera.launch(uri)
            },
            modifier = Modifier.fillMaxWidth().height(72.dp),
        ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.add_take_photo))
        }
        OutlinedButton(
            onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.add_pick_photo))
        }
        TextButton(onClick = onManual, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.add_manual))
        }
    }
}

@Composable
private fun Processing() {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(Modifier.size(72.dp))
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.add_processing), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.add_processing_hint), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Failed(message: String, onRetry: () -> Unit, onManual: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(64.dp)) {
            Text(stringResource(R.string.add_retry))
        }
        OutlinedButton(onClick = onManual, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(stringResource(R.string.add_manual))
        }
    }
}

@Composable
private fun Review(state: AddState.Review, viewModel: AddPrescriptionViewModel) {
    val focusManager = LocalFocusManager.current
    LazyColumn(
        modifier = Modifier.pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text(stringResource(R.string.review_intro), style = MaterialTheme.typography.bodyLarge) }
        items(state.warnings) { warning -> WarningCard(warning) }
        items(state.drafts, key = { it.key }) { draft ->
            DraftCard(
                draft = draft,
                showMissing = state.showMissing,
                onChange = viewModel::updateDraft,
                onRemove = { viewModel.removeDraft(draft.key) },
            )
        }
        item {
            OutlinedButton(onClick = viewModel::addDraft, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.review_add_medicine))
            }
        }
        item {
            if (state.showMissing && !state.drafts.all { it.isComplete }) {
                Text(stringResource(R.string.review_missing), color = MaterialTheme.colorScheme.error)
            }
            // The whole row is the tap target, not just the small box.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = state.confirmed,
                        role = Role.Checkbox,
                        onValueChange = viewModel::setConfirmed,
                    )
                    .padding(vertical = 8.dp),
            ) {
                Checkbox(checked = state.confirmed, onCheckedChange = null)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.review_confirm), style = MaterialTheme.typography.bodyLarge)
            }
            Button(
                onClick = viewModel::save,
                enabled = state.confirmed && state.drafts.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(64.dp),
            ) {
                Text(stringResource(R.string.review_save))
            }
        }
    }
}

@Composable
private fun WarningCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun DraftCard(
    draft: DraftMedication,
    showMissing: Boolean,
    onChange: (DraftMedication) -> Unit,
    onRemove: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val border = if (draft.needsCheck) BorderStroke(3.dp, MaterialTheme.colorScheme.error) else null
    Card(border = border, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (draft.needsCheck) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.review_check) + if (draft.notes.isNotBlank()) ": ${draft.notes}" else "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            OutlinedTextField(
                value = draft.name,
                onValueChange = { onChange(draft.copy(name = it)) },
                label = { Text(stringResource(R.string.review_name)) },
                isError = showMissing && draft.name.isBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = draft.dose,
                onValueChange = { onChange(draft.copy(dose = it)) },
                label = { Text(stringResource(R.string.review_dose)) },
                isError = showMissing && draft.dose.isBlank(),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.review_times), style = MaterialTheme.typography.titleMedium)
            if (draft.timesSuggested) {
                Text(stringResource(R.string.review_times_suggested), style = MaterialTheme.typography.bodyMedium)
            }
            TimesEditor(
                times = draft.times,
                onChange = { onChange(draft.copy(times = it)) },
                // Otherwise the last text field regains focus and the keyboard covers the form.
                beforePick = { focusManager.clearFocus() },
            )
            if (showMissing && draft.times.isEmpty()) {
                Text(stringResource(R.string.review_missing), color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = draft.durationDays,
                onValueChange = { value -> onChange(draft.copy(durationDays = value.filter(Char::isDigit).take(4))) },
                label = { Text(stringResource(R.string.review_duration)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = draft.instructions,
                onValueChange = { onChange(draft.copy(instructions = it)) },
                label = { Text(stringResource(R.string.review_instructions)) },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onRemove) {
                Text(stringResource(R.string.review_remove_medicine), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
