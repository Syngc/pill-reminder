package com.pillreminder.app

import com.pillreminder.app.alarm.DoseMessage
import com.pillreminder.app.data.AppLanguage
import com.pillreminder.app.data.Medication
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DoseMessageTest {
    private fun med(name: String, dose: String, instructions: String = "") =
        Medication(0, name, dose, listOf("08:00"), instructions, LocalDate.now(), null)

    private val one = listOf(med("Losartán", "50 mg", "Tomar con agua."))
    private val two = listOf(med("Losartán", "50 mg"), med("Metformina", "1 tableta"))

    @Test fun spanishSingle() {
        assertEquals(
            "Es hora de tomar tu medicina. Losartán, 50 mg. Tomar con agua. " +
                "Cuando termines, toca el botón verde que dice: ya me la tomé.",
            DoseMessage.spoken(one, AppLanguage.SPANISH),
        )
    }

    @Test fun spanishPlural() {
        assertEquals(
            "Es hora de tomar tus medicinas. Losartán, 50 mg. Metformina, 1 tableta. " +
                "Cuando termines, toca el botón verde que dice: ya me las tomé.",
            DoseMessage.spoken(two, AppLanguage.SPANISH),
        )
    }

    @Test fun englishSingle() {
        assertEquals(
            "It's time to take your medicine. Losartan, 50 mg. Take with water. " +
                "When you're done, tap the green button that says: i took it.",
            DoseMessage.spoken(listOf(med("Losartan", "50 mg", "Take with water")), AppLanguage.ENGLISH),
        )
    }

    @Test fun englishPlural() {
        assertEquals(
            "It's time to take your medicines. Losartán, 50 mg. Metformina, 1 tableta. " +
                "When you're done, tap the green button that says: i took them.",
            DoseMessage.spoken(two, AppLanguage.ENGLISH),
        )
    }

    @Test fun buttonLabels() {
        assertEquals("YA ME LA TOMÉ", DoseMessage.confirmLabel(1, AppLanguage.SPANISH))
        assertEquals("YA ME LAS TOMÉ", DoseMessage.confirmLabel(3, AppLanguage.SPANISH))
        assertEquals("I TOOK IT", DoseMessage.confirmLabel(1, AppLanguage.ENGLISH))
        assertEquals("I TOOK THEM", DoseMessage.confirmLabel(2, AppLanguage.ENGLISH))
    }
}
