package com.pillreminder.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pillreminder.app.R
import com.pillreminder.app.data.Medication
import com.pillreminder.app.ui.home.SlotStatus
import com.pillreminder.app.ui.theme.AppColors

/** Title row with the round back button used on every secondary screen. */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    val colors = AppColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = colors.surface,
            border = BorderStroke(1.5.dp, colors.border),
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(AppIcons.Back, contentDescription = stringResource(R.string.back), tint = colors.ink, modifier = Modifier.size(28.dp))
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.ink,
            modifier = Modifier.semantics { heading() },
        )
    }
}

/** The one main action of a screen: filled, tall, with a soft accent shadow. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 76.dp,
    enabled: Boolean = true,
    textStyle: TextStyle = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
    container: Color = AppColors.current.accent,
    content: Color = Color.White,
) {
    val shape = RoundedCornerShape(22.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = AppColors.current.border,
            disabledContentColor = AppColors.current.muted,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .then(
                if (enabled) Modifier.shadow(10.dp, shape, ambientColor = container, spotColor = container) else Modifier
            ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = textStyle)
    }
}

/** A quieter action: white with a thin border. [accent] gives it the accent outline instead. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 60.dp,
    accent: Boolean = false,
) {
    val colors = AppColors.current
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(if (accent) 2.dp else 1.5.dp, if (accent) colors.accentText else colors.border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surface,
            contentColor = if (accent) colors.accentText else colors.ink,
        ),
        contentPadding = PaddingValues(horizontal = 14.dp),
        modifier = modifier.height(height),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (accent) FontWeight.Bold else FontWeight.SemiBold),
            maxLines = 2,
        )
    }
}

/** A dashed "add something" button. */
@Composable
fun DashedButton(text: String?, onClick: () -> Unit, modifier: Modifier = Modifier, contentDescription: String? = null) {
    val colors = AppColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        contentColor = colors.accentText,
        modifier = modifier.dashedBorder(colors.accentText.copy(alpha = 0.6f), 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 14.dp),
        ) {
            Icon(AppIcons.Plus, contentDescription = contentDescription, modifier = Modifier.size(24.dp))
            if (text != null) {
                Spacer(Modifier.width(8.dp))
                Text(text, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

/** Drawn on top of the content, so a component's own background can't hide it. */
fun Modifier.dashedBorder(color: Color, radius: Dp, width: Dp = 1.5.dp): Modifier = drawWithContent {
    drawContent()
    val stroke = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
        size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))),
    )
}

/** White rounded card on the tinted background. */
@Composable
fun AppCard(modifier: Modifier = Modifier, radius: Dp = 26.dp, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(radius),
        color = AppColors.current.surface,
        contentColor = AppColors.current.ink,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

/** Status as icon + word + color, never color alone. */
@Composable
fun StatusBadge(status: SlotStatus) {
    val colors = AppColors.current
    val (label, icon, pair) = when (status) {
        SlotStatus.TAKEN -> Triple(R.string.status_taken, AppIcons.Check, colors.tint to colors.chipText)
        SlotStatus.NOW -> Triple(R.string.status_now, AppIcons.Bell, colors.accent to Color.White)
        SlotStatus.MISSED -> Triple(R.string.status_missed, AppIcons.Alert, colors.warningChip to colors.warningText)
        SlotStatus.UPCOMING -> Triple(R.string.status_upcoming, AppIcons.Clock, colors.laterChip to colors.laterText)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(pair.first, RoundedCornerShape(16.dp))
            .padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = pair.second, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = pair.second)
    }
}

/** A time of day shown as a chip; tappable when [onClick] is set, removable when [onRemove] is set. */
@Composable
fun TimeChip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    removeLabel: String? = null,
) {
    val colors = AppColors.current
    val body: @Composable RowScope.() -> Unit = {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = colors.chipText,
            modifier = Modifier.padding(start = 14.dp, end = if (onRemove == null) 14.dp else 0.dp),
        )
        if (onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.size(44.dp)) {
                Icon(AppIcons.Close, contentDescription = removeLabel, tint = colors.chipText, modifier = Modifier.size(20.dp))
            }
        }
    }
    val shape = RoundedCornerShape(14.dp)
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = colors.chip, modifier = modifier.heightIn(min = 48.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 48.dp), content = body)
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.background(colors.chip, shape).heightIn(min = 40.dp),
            content = body,
        )
    }
}

enum class BannerKind { WARNING, INFO }

@Composable
fun Banner(text: String, kind: BannerKind, modifier: Modifier = Modifier, boldPrefix: String? = null) {
    val colors = AppColors.current
    val (bg, fg, icon) = when (kind) {
        BannerKind.WARNING -> Triple(colors.warning, colors.warningText, AppIcons.Alert)
        BannerKind.INFO -> Triple(colors.chip, colors.chipText, AppIcons.Info)
    }
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(24.dp).padding(top = 1.dp))
        Spacer(Modifier.width(10.dp))
        val body = if (boldPrefix != null) {
            androidx.compose.ui.text.buildAnnotatedString {
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
                append(boldPrefix)
                pop()
                append(" ")
                append(text)
            }
        } else {
            androidx.compose.ui.text.AnnotatedString(text)
        }
        Text(body, style = MaterialTheme.typography.bodyMedium, color = fg)
    }
}

/** The tinted square with a capsule or tablet icon, chosen from how the medicine is described. */
@Composable
fun MedicineIcon(medication: Medication, size: Dp = 56.dp) {
    val colors = AppColors.current
    val tablet = listOf(medication.name, medication.dose).any {
        val lower = it.lowercase()
        "tablet" in lower || "tab " in "$lower "
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size).background(colors.tint, RoundedCornerShape(size * 0.32f)),
    ) {
        Icon(
            if (tablet) AppIcons.Tablet else AppIcons.Capsule,
            contentDescription = null,
            tint = colors.accentText,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

/** "Step 1 of 3 · Photo" with three bars. */
@Composable
fun StepProgress(step: Int, label: String) {
    val colors = AppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(if (index < step) colors.accentText else colors.border, RoundedCornerShape(4.dp))
                )
            }
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
    }
}

/** Label above a large text field, as in the review screen. */
@Composable
fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    emphasize: Boolean = false,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 21.sp),
) {
    val colors = AppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 17.sp), color = colors.muted)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            isError = isError,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 2,
            textStyle = textStyle,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = if (emphasize) colors.warningBorder else colors.border,
                focusedBorderColor = colors.accentText,
                unfocusedContainerColor = colors.surface,
                focusedContainerColor = colors.surface,
                focusedTextColor = colors.ink,
                unfocusedTextColor = colors.ink,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A large checkbox with its label; the whole row is the tap target. */
@Composable
fun CheckRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit, text: String, modifier: Modifier = Modifier, boxed: Boolean = false) {
    val colors = AppColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (boxed) {
                    Modifier
                        .background(colors.surface, RoundedCornerShape(20.dp))
                        .border(1.5.dp, colors.border, RoundedCornerShape(20.dp))
                } else Modifier
            )
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(if (boxed) 14.dp else 4.dp)
            .heightIn(min = 44.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = colors.accentText, uncheckedColor = colors.muted),
        )
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp), color = colors.ink)
    }
}
