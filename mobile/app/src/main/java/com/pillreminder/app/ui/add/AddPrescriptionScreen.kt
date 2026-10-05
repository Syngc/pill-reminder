package com.pillreminder.app.ui.add

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pillreminder.app.R
import com.pillreminder.app.ui.TimesEditor
import com.pillreminder.app.ui.components.AppCard
import com.pillreminder.app.ui.components.AppIcons
import com.pillreminder.app.ui.components.Banner
import com.pillreminder.app.ui.components.BannerKind
import com.pillreminder.app.ui.components.CheckRow
import com.pillreminder.app.ui.components.DashedButton
import com.pillreminder.app.ui.components.LabeledField
import com.pillreminder.app.ui.components.PrimaryButton
import com.pillreminder.app.ui.components.ScreenHeader
import com.pillreminder.app.ui.components.SecondaryButton
import com.pillreminder.app.ui.components.StepProgress
import com.pillreminder.app.ui.components.dashedBorder
import com.pillreminder.app.ui.theme.AppColors
import java.io.File

@Composable
fun AddPrescriptionScreen(
    onDone: () -> Unit,
    viewModel: AddPrescriptionViewModel = viewModel(factory = AddPrescriptionViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state) { if (state is AddState.Saved) onDone() }

    val goBack = { if (state is AddState.Review || state is AddState.Failed) viewModel.reset() else onDone() }
    BackHandler(onBack = goBack)

    val colors = AppColors.current
    Scaffold(containerColor = if (state is AddState.Processing) colors.surface else colors.background) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                AddState.Choose -> ChooseSource(onBack = goBack, onPhoto = viewModel::extract, onManual = viewModel::startManual)
                AddState.Processing -> Processing(onBack = goBack)
                is AddState.Failed -> Failed(
                    message = s.message ?: stringResource(s.fallback),
                    onBack = goBack,
                    onRetry = viewModel::reset,
                    onManual = viewModel::startManual,
                )
                is AddState.Review -> Review(s, viewModel, onBack = goBack)
                AddState.Saved -> Unit
            }
        }
    }
}

private val ScreenPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 28.dp)

/** Step 1: photo tips and the three ways to start. */
@Composable
private fun ChooseSource(onBack: () -> Unit, onPhoto: (Uri) -> Unit, onManual: () -> Unit) {
    val context = LocalContext.current
    val colors = AppColors.current
    var pendingPhoto by rememberSaveable { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingPhoto
        if (saved && uri != null) onPhoto(uri)
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPhoto(uri)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(ScreenPadding),
    ) {
        ScreenHeader(stringResource(R.string.add_title), onBack)
        StepProgress(1, stringResource(R.string.step_photo))
        AppCard {
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                PaperIllustration(width = 220.dp, height = 260.dp, framed = true)
            }
            listOf(R.string.tip_light, R.string.tip_frame, R.string.tip_check).forEach { tip ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(AppIcons.Check, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(tip), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp), color = colors.ink)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        PrimaryButton(
            text = stringResource(R.string.add_take_photo),
            onClick = {
                val dir = File(context.cacheDir, "photos").apply { mkdirs() }
                val file = File(dir, "prescription-${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                pendingPhoto = uri
                camera.launch(uri)
            },
            icon = AppIcons.Camera,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(
                text = stringResource(R.string.add_pick_photo),
                onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                icon = AppIcons.Gallery,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = stringResource(R.string.add_manual),
                onClick = onManual,
                icon = AppIcons.Pencil,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Step 2: the prescription being read, with a moving scan line. */
@Composable
private fun Processing(onBack: () -> Unit) {
    val colors = AppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxSize().padding(ScreenPadding)) {
        ScreenHeader(stringResource(R.string.add_title), onBack)
        StepProgress(2, stringResource(R.string.step_reading))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            PaperIllustration(width = 200.dp, height = 236.dp, scanning = true)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.add_processing),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(R.string.add_processing_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.muted,
                    textAlign = TextAlign.Center,
                )
            }
            LoadingDots()
        }
        Text(
            stringResource(R.string.reading_footer),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun Failed(message: String, onBack: () -> Unit, onRetry: () -> Unit, onManual: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(ScreenPadding),
    ) {
        ScreenHeader(stringResource(R.string.add_title), onBack)
        StepProgress(2, stringResource(R.string.step_reading))
        Banner(message, BannerKind.WARNING)
        Spacer(Modifier.height(8.dp))
        PrimaryButton(stringResource(R.string.add_retry), onRetry, icon = AppIcons.Camera)
        SecondaryButton(stringResource(R.string.add_manual), onManual, icon = AppIcons.Pencil, modifier = Modifier.fillMaxWidth())
    }
}

/** Step 3: compare every detail with the paper before any alarm is turned on. */
@Composable
private fun Review(state: AddState.Review, viewModel: AddPrescriptionViewModel, onBack: () -> Unit) {
    val colors = AppColors.current
    val focusManager = LocalFocusManager.current
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
    ) {
        item { ScreenHeader(stringResource(R.string.review_title), onBack) }
        item { StepProgress(3, stringResource(R.string.step_check)) }
        item {
            Text(stringResource(R.string.review_intro), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp), color = colors.muted)
        }
        items(state.warnings) { warning -> Banner(warning, BannerKind.WARNING) }
        items(state.drafts, key = { it.key }) { draft ->
            DraftCard(
                draft = draft,
                showMissing = state.showMissing,
                onChange = viewModel::updateDraft,
                onRemove = { viewModel.removeDraft(draft.key) },
            )
        }
        item {
            DashedButton(
                text = stringResource(R.string.review_add_medicine),
                onClick = viewModel::addDraft,
                modifier = Modifier.fillMaxWidth().height(60.dp),
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (state.showMissing && !state.drafts.all { it.isComplete }) {
                    Text(stringResource(R.string.review_missing), style = MaterialTheme.typography.bodyLarge, color = colors.danger)
                }
                CheckRow(
                    checked = state.confirmed,
                    onCheckedChange = viewModel::setConfirmed,
                    text = stringResource(R.string.review_confirm),
                    boxed = true,
                )
                PrimaryButton(
                    text = stringResource(R.string.review_save),
                    onClick = viewModel::save,
                    enabled = state.confirmed && state.drafts.isNotEmpty(),
                    icon = AppIcons.Bell,
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
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
    val colors = AppColors.current
    val focusManager = LocalFocusManager.current
    AppCard(radius = 24.dp) {
        // Orange only when the AI wasn't sure what it read; other notes are informational.
        if (draft.lowConfidence) {
            Banner(
                text = draft.notes.ifBlank { stringResource(R.string.review_low_confidence) },
                kind = BannerKind.WARNING,
                boldPrefix = stringResource(R.string.review_check),
            )
        } else if (draft.notes.isNotBlank() && !draft.timesSuggested) {
            Banner(draft.notes, BannerKind.INFO)
        }
        LabeledField(
            label = stringResource(R.string.review_name),
            value = draft.name,
            onValueChange = { onChange(draft.copy(name = it)) },
            isError = showMissing && draft.name.isBlank(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 21.sp, fontWeight = FontWeight.SemiBold),
        )
        LabeledField(
            label = stringResource(R.string.review_dose),
            value = draft.dose,
            onValueChange = { onChange(draft.copy(dose = it)) },
            isError = showMissing && draft.dose.isBlank(),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.review_times), style = MaterialTheme.typography.labelSmall.copy(fontSize = 17.sp), color = colors.muted)
            if (draft.timesSuggested) {
                // The AI's note explains where the suggested times came from ("every 8 hours").
                Banner(draft.notes.ifBlank { stringResource(R.string.review_times_suggested) }, BannerKind.INFO)
            }
            TimesEditor(
                times = draft.times,
                onChange = { onChange(draft.copy(times = it)) },
                // Otherwise the last text field regains focus and the keyboard covers the form.
                beforePick = { focusManager.clearFocus() },
            )
            if (showMissing && draft.times.isEmpty()) {
                Text(stringResource(R.string.review_missing), style = MaterialTheme.typography.bodyMedium, color = colors.danger)
            }
        }
        LabeledField(
            label = stringResource(R.string.review_duration),
            value = draft.durationDays,
            onValueChange = { value -> onChange(draft.copy(durationDays = value.filter(Char::isDigit).take(4))) },
            keyboardType = KeyboardType.Number,
        )
        LabeledField(
            label = stringResource(R.string.review_instructions),
            value = draft.instructions,
            onValueChange = { onChange(draft.copy(instructions = it)) },
            singleLine = false,
            textStyle = MaterialTheme.typography.bodyLarge,
        )
        TextButton(onClick = onRemove, modifier = Modifier.height(44.dp)) {
            Text(stringResource(R.string.review_remove_medicine), style = MaterialTheme.typography.titleSmall, color = colors.danger)
        }
    }
}

/** A stylized prescription sheet: framed for the camera tips, or with a moving scan line while reading. */
@Composable
private fun PaperIllustration(width: Dp, height: Dp, framed: Boolean = false, scanning: Boolean = false) {
    val colors = AppColors.current
    val shape = RoundedCornerShape(if (framed) 16.dp else 18.dp)
    Box(Modifier.size(width, height)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background, shape)
                .then(if (framed) Modifier.dashedBorder(colors.border, 16.dp, 3.dp) else Modifier.border(1.5.dp, colors.border, shape))
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            SheetLine(0.6f, 12.dp, colors.border)
            listOf(0.9f, 0.8f, 0.85f, 0.5f, 0.75f).forEach { SheetLine(it, 10.dp, colors.chip) }
        }
        if (framed) CornerBrackets(colors.accentText)
        if (scanning) ScanLine(height, colors.accentText)
    }
}

@Composable
private fun SheetLine(fraction: Float, thickness: Dp, color: Color) {
    Box(Modifier.fillMaxWidth(fraction).height(thickness).background(color, RoundedCornerShape(thickness / 2)))
}

@Composable
private fun BoxScope.CornerBrackets(color: Color) {
    val corners = listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd)
    corners.forEach { corner ->
        val left = corner == Alignment.TopStart || corner == Alignment.BottomStart
        val top = corner == Alignment.TopStart || corner == Alignment.TopEnd
        Box(
            Modifier
                .align(corner)
                .offset(x = if (left) (-10).dp else 10.dp, y = if (top) (-10).dp else 10.dp)
                .size(28.dp)
                .drawBehind {
                    val w = 5.dp.toPx()
                    val y = if (top) w / 2 else size.height - w / 2
                    val x = if (left) w / 2 else size.width - w / 2
                    drawLine(color, Offset(0f, y), Offset(size.width, y), w, StrokeCap.Round)
                    drawLine(color, Offset(x, 0f), Offset(x, size.height), w, StrokeCap.Round)
                },
        )
    }
}

@Composable
private fun BoxScope.ScanLine(height: Dp, color: Color) {
    val transition = rememberInfiniteTransition(label = "scan")
    val progress by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "scan-position",
    )
    Box(Modifier.align(Alignment.TopCenter).offset(y = height * progress).fillMaxWidth().height(4.dp).background(color))
}

@Composable
private fun LoadingDots() {
    val colors = AppColors.current
    val transition = rememberInfiniteTransition(label = "dots")
    val phase by transition.animateFloat(0f, 3f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "dots-phase")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) { index ->
            Box(
                Modifier
                    .size(12.dp)
                    .alpha(if (phase.toInt() == index) 1f else 0.35f)
                    .background(colors.accentText, CircleShape),
            )
        }
    }
}
