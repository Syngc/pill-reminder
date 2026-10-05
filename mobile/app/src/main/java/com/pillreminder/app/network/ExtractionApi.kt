package com.pillreminder.app.network

import com.pillreminder.app.data.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

@Serializable
data class ExtractedMedication(
    val name: String,
    val dose: String,
    val times: List<String>,
    @SerialName("times_are_suggested") val timesAreSuggested: Boolean,
    @SerialName("duration_days") val durationDays: Int? = null,
    val instructions: String,
    val confidence: String,
    @SerialName("notes_for_reviewer") val notesForReviewer: String,
)

@Serializable
data class ExtractionResult(
    val readable: Boolean,
    val medications: List<ExtractedMedication>,
    val warnings: List<String>,
)

/** Why extraction failed. The screen turns each case into text in the app language. */
sealed class ExtractionException : Exception() {
    class NoConnection : ExtractionException()
    class BadResponse : ExtractionException()

    /** The backend's own explanation, already in the language that was requested. */
    class Server(val detail: String?) : ExtractionException()
}

class ExtractionApi(
    private val baseUrl: String,
    private val apiKey: String,
    private val http: OkHttpClient = defaultClient(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Reads the prescription in [jpeg]. Instructions, notes and errors come back in [language]. */
    suspend fun extract(jpeg: ByteArray, language: AppLanguage): ExtractionResult = withContext(Dispatchers.IO) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", "prescription.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))
            .build()
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/v1/prescriptions/extract")
            .post(body)
            .header("Accept-Language", language.tag)
            .apply { if (apiKey.isNotEmpty()) header("X-API-Key", apiKey) }
            .build()

        val response = try {
            http.newCall(request).execute()
        } catch (e: IOException) {
            throw ExtractionException.NoConnection()
        }
        response.use {
            val text = it.body?.string().orEmpty()
            if (!it.isSuccessful) throw ExtractionException.Server(errorDetail(text))
            try {
                json.decodeFromString<ExtractionResult>(text)
            } catch (e: SerializationException) {
                throw ExtractionException.BadResponse()
            } catch (e: IllegalArgumentException) {
                throw ExtractionException.BadResponse()
            }
        }
    }

    private fun errorDetail(body: String): String? = runCatching {
        json.parseToJsonElement(body).jsonObject["detail"]?.jsonPrimitive?.content
    }.getOrNull()

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            // Reading a prescription can take a while; keep the call open.
            .readTimeout(150, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
