package com.pillreminder.app

import com.pillreminder.app.data.AppLanguage
import com.pillreminder.app.network.ExtractionApi
import com.pillreminder.app.network.ExtractionException
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ExtractionApiTest {
    private lateinit var server: MockWebServer

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun api(key: String = "") = ExtractionApi(server.url("/").toString(), key)

    @Test fun parsesMedicationsAndSendsKey() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"readable":true,"warnings":[],"medications":[{"name":"Losartán","dose":"50 mg",
                "times":["08:00"],"times_are_suggested":false,"duration_days":null,"instructions":"",
                "confidence":"high","notes_for_reviewer":""}]}"""
            )
        )
        val result = api("secret").extract(byteArrayOf(1, 2, 3), AppLanguage.ENGLISH)
        assertEquals("Losartán", result.medications.single().name)

        val request = server.takeRequest()
        assertEquals("/v1/prescriptions/extract", request.path)
        assertEquals("secret", request.getHeader("X-API-Key"))
        assertEquals("en", request.getHeader("Accept-Language"))
        assertTrue(request.getHeader("Content-Type")!!.startsWith("multipart/form-data"))
    }

    @Test fun surfacesServerErrorDetail() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(422).setBody("""{"detail":"No se pudo leer esta receta."}"""))
        try {
            api().extract(byteArrayOf(1), AppLanguage.SPANISH)
            fail("expected ExtractionException")
        } catch (e: ExtractionException.Server) {
            assertEquals("No se pudo leer esta receta.", e.detail)
        }
        assertEquals("es", server.takeRequest().getHeader("Accept-Language"))
    }

    @Test fun malformedBodyIsBadResponse() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"readable": true}"""))
        try {
            api().extract(byteArrayOf(1), AppLanguage.SPANISH)
            fail("expected ExtractionException")
        } catch (e: ExtractionException.BadResponse) {
            // expected
        }
    }

    @Test fun unreachableServerIsNoConnection() = runBlocking {
        val url = server.url("/").toString()
        server.shutdown()
        try {
            ExtractionApi(url, "").extract(byteArrayOf(1), AppLanguage.SPANISH)
            fail("expected ExtractionException")
        } catch (e: ExtractionException.NoConnection) {
            // expected
        }
    }
}
