package com.pillreminder.app

import com.pillreminder.app.data.Medication
import com.pillreminder.app.ui.home.HomeViewModel.Companion.matchesSearch
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SearchTest {
    private val acetaminofen = Medication(
        1, "Acetaminofén 500 mg tabletas", "2 tabletas", listOf("08:00"),
        "Tomar con el estómago lleno.", LocalDate.now(), null,
    )
    private val jarabe = Medication(
        2, "Acebrofilina jarabe 50 mg/5 ml", "10 ml", listOf("08:00"),
        "Vía oral. Nombre comercial FILINAR.", LocalDate.now(), null,
    )

    @Test fun emptyOrBlankSearchShowsEverything() {
        assertTrue(matchesSearch(acetaminofen, ""))
        assertTrue(matchesSearch(acetaminofen, "   "))
    }

    @Test fun ignoresCaseAndAccents() {
        assertTrue(matchesSearch(acetaminofen, "acetaminofen"))
        assertTrue(matchesSearch(acetaminofen, "ACETAMINOFÉN"))
        assertTrue(matchesSearch(acetaminofen, " aceta "))
    }

    @Test fun findsPartialNamesAndInstructions() {
        assertTrue(matchesSearch(jarabe, "jarabe"))
        assertTrue(matchesSearch(jarabe, "filinar"))
        assertTrue(matchesSearch(acetaminofen, "estomago"))
    }

    @Test fun nonMatchingSearchIsFiltered() {
        assertFalse(matchesSearch(acetaminofen, "losartan"))
        assertFalse(matchesSearch(jarabe, "tabletas"))
    }
}
