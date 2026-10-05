package com.pillreminder.app.alarm

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.util.Log
import com.pillreminder.app.data.AppLanguage
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Text-to-speech in the app language on the alarm audio stream, with suspend-until-done speaking. */
class Speaker(context: Context, private val language: AppLanguage) : TextToSpeech.OnInitListener {
    private val ready = CompletableDeferred<Boolean>()
    private val inFlight = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
    private val tts = TextToSpeech(context.applicationContext, this)

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            ready.complete(false)
            return
        }
        val locale = candidateLocales(language).firstOrNull { tts.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }
        if (locale == null) {
            Log.w(TAG, "No $language voice available; alarms will ring without speech")
            ready.complete(false)
            return
        }
        val result = tts.setLanguage(locale)
        Log.i(TAG, "Speaking as $locale (setLanguage=$result, voice=${tts.voice?.name})")
        tts.setSpeechRate(0.85f)
        tts.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) = Unit
            override fun onDone(utteranceId: String) = finish(utteranceId)
            override fun onStop(utteranceId: String, interrupted: Boolean) = finish(utteranceId)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) = finish(utteranceId)
        })
        ready.complete(true)
    }

    private fun finish(id: String) {
        inFlight.remove(id)?.complete(Unit)
    }

    /** Speaks [text] and returns when it has finished. Returns immediately if no voice is available. */
    suspend fun speak(text: String) {
        val available = withTimeoutOrNull(5_000) { ready.await() } ?: false
        if (!available) return
        val id = UUID.randomUUID().toString()
        val done = CompletableDeferred<Unit>()
        inFlight[id] = done
        try {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
            withTimeoutOrNull(90_000) { done.await() }
        } finally {
            if (!done.isCompleted) {
                inFlight.remove(id)
                tts.stop()
            }
        }
    }

    fun shutdown() = tts.shutdown()

    companion object {
        private const val TAG = "Speaker"

        /** Preferred regional voices first, then the bare language. */
        fun candidateLocales(language: AppLanguage): List<Locale> = when (language) {
            AppLanguage.SPANISH -> listOf("es-MX", "es-US", "es-419", "es-ES", "es")
            AppLanguage.ENGLISH -> listOf("en-US", "en-GB", "en")
        }.map(Locale::forLanguageTag)
    }
}
