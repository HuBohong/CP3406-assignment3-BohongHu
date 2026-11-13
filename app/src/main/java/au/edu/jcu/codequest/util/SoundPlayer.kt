package au.edu.jcu.codequest.util

import android.media.AudioManager
import android.media.ToneGenerator
import javax.inject.Inject
import javax.inject.Singleton

/** Plays a short cue when an answer is checked. */
interface SoundPlayer {
    fun play(correct: Boolean)
}

/** Uses the system tone generator, so the app ships no audio files and needs no permission. */
@Singleton
class ToneSoundPlayer @Inject constructor() : SoundPlayer {

    // Created on first use. Some devices refuse to create a ToneGenerator; sound is then skipped.
    private val toneGenerator: ToneGenerator? by lazy {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME)
        } catch (e: RuntimeException) {
            null
        }
    }

    override fun play(correct: Boolean) {
        val tone = if (correct) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_NACK
        toneGenerator?.startTone(tone, DURATION_MS)
    }

    private companion object {
        const val VOLUME = 60
        const val DURATION_MS = 150
    }
}
