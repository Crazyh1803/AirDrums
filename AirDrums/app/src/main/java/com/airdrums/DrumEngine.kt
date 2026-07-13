package com.airdrums

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class DrumEngine(context: Context) {

    enum class Drum { SNARE, HI_HAT, TOM1, TOM2, CRASH, RIDE, BASS }

    private val soundPool: SoundPool
    private val soundIds = mutableMapOf<Drum, Int>()
    private val loadedCount = AtomicInteger(0)
    private val totalSounds = Drum.values().size

    var onAllLoaded: (() -> Unit)? = null

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attrs)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                if (loadedCount.incrementAndGet() == totalSounds) {
                    onAllLoaded?.invoke()
                }
            }
        }
    }

    fun loadSounds(cacheDir: File) {
        val files = SoundGenerator.generateAll(cacheDir)
        for ((drum, file) in files) {
            val id = soundPool.load(file.absolutePath, 1)
            soundIds[drum] = id
        }
    }

    fun hit(drum: Drum, velocity: Float = 1.0f) {
        soundIds[drum]?.let { id ->
            soundPool.play(id, velocity, velocity, 1, 0, 1.0f)
        }
    }

    fun release() {
        soundPool.release()
    }
}
