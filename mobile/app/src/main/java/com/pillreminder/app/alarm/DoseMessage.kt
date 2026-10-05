package com.pillreminder.app.alarm

import com.pillreminder.app.data.AppLanguage
import com.pillreminder.app.data.Medication

/**
 * What the alarm says out loud and the label of its one button. It only repeats what was
 * confirmed from the prescription. Kept in code rather than string resources so the
 * grammar (singular/plural) for each language is explicit and unit-tested.
 */
object DoseMessage {
    fun confirmLabel(count: Int, language: AppLanguage): String = when (language) {
        AppLanguage.SPANISH -> if (count == 1) "YA ME LA TOMÉ" else "YA ME LAS TOMÉ"
        AppLanguage.ENGLISH -> if (count == 1) "I TOOK IT" else "I TOOK THEM"
    }

    fun spoken(medications: List<Medication>, language: AppLanguage): String = buildString {
        val plural = medications.size != 1
        append(
            when (language) {
                AppLanguage.SPANISH -> if (plural) "Es hora de tomar tus medicinas. " else "Es hora de tomar tu medicina. "
                AppLanguage.ENGLISH -> if (plural) "It's time to take your medicines. " else "It's time to take your medicine. "
            }
        )
        for (med in medications) {
            append(sentence("${med.name}, ${med.dose}"))
            if (med.instructions.isNotBlank()) append(sentence(med.instructions))
        }
        // Describe the button by its size and words, never its color: colors change with the design and theme.
        append(
            when (language) {
                AppLanguage.SPANISH -> "Cuando termines, toca el botón grande que dice: "
                AppLanguage.ENGLISH -> "When you're done, tap the big button that says: "
            }
        )
        val label = confirmLabel(medications.size, language).lowercase(language.locale)
        // English keeps "I" capitalized ("I took it").
        append(if (language == AppLanguage.ENGLISH) label.replaceFirstChar { it.titlecase(language.locale) } else label)
        append(".")
    }

    fun test(language: AppLanguage): String = when (language) {
        AppLanguage.SPANISH -> "Esta es una prueba de la alarma. Así sonará a la hora de tu medicina."
        AppLanguage.ENGLISH -> "This is a test of the alarm. This is how it will sound when it's time for your medicine."
    }

    fun confirmed(language: AppLanguage): String = when (language) {
        AppLanguage.SPANISH -> "Muy bien. Quedó registrado."
        AppLanguage.ENGLISH -> "Well done. It's been recorded."
    }

    private fun sentence(text: String) = text.trim().trimEnd('.') + ". "
}
