package com.joshw.voicereader.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class AndroidTtsEngine(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false

    var onUtteranceCompleted: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                isReady = true
                setupListener()
            }
        }
    }

    private fun setupListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                onUtteranceCompleted?.invoke()
            }
            override fun onError(utteranceId: String?) {}
        })
    }

    fun speak(text: String) {
        if (!isReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utterance_id")
    }

    fun stop() {
        tts?.stop()
    }

    fun setSpeed(speed: Float) {
        tts?.setSpeechRate(speed)
    }

    fun isReady() = isReady

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}