package com.airdrums

import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

object SoundGenerator {

    private const val SAMPLE_RATE = 44100

    fun generateAll(cacheDir: File): Map<DrumEngine.Drum, File> {
        return mapOf(
            DrumEngine.Drum.SNARE  to writePcm(cacheDir, "snare.wav",  generateSnare()),
            DrumEngine.Drum.HI_HAT to writePcm(cacheDir, "hihat.wav",  generateHiHat()),
            DrumEngine.Drum.TOM1   to writePcm(cacheDir, "tom1.wav",   generateTom(120.0)),
            DrumEngine.Drum.TOM2   to writePcm(cacheDir, "tom2.wav",   generateTom(90.0)),
            DrumEngine.Drum.CRASH  to writePcm(cacheDir, "crash.wav",  generateCrash()),
            DrumEngine.Drum.RIDE   to writePcm(cacheDir, "ride.wav",   generateRide()),
            DrumEngine.Drum.BASS   to writePcm(cacheDir, "bass.wav",   generateBass())
        )
    }

    private fun generateSnare(): ShortArray {
        val samples = msToSamples(80)
        val rng = Random(42)
        return ShortArray(samples) { i ->
            val t = i.toDouble() / samples
            val decay = exp(-t * 6.0)
            val noise = rng.nextDouble() * 2.0 - 1.0
            val tone = sin(2 * PI * 200 * i / SAMPLE_RATE) * 0.3
            pcmClamp((noise + tone) * decay * 0.85)
        }
    }

    private fun generateHiHat(): ShortArray {
        val samples = msToSamples(30)
        val rng = Random(7)
        return ShortArray(samples) { i ->
            val t = i.toDouble() / samples
            val decay = exp(-t * 12.0)
            val noise = rng.nextDouble() * 2.0 - 1.0
            pcmClamp(noise * decay * 0.7)
        }
    }

    private fun generateTom(freq: Double): ShortArray {
        val samples = msToSamples(150)
        return ShortArray(samples) { i ->
            val t = i.toDouble() / samples
            val decay = exp(-t * 5.0)
            val pitch = freq * (1.0 + (1.0 - t) * 0.5)  // pitch drop for realism
            val tone = sin(2 * PI * pitch * i / SAMPLE_RATE)
            pcmClamp(tone * decay * 0.9)
        }
    }

    private fun generateCrash(): ShortArray {
        val samples = msToSamples(120)
        val rng = Random(13)
        return ShortArray(samples) { i ->
            val t = i.toDouble() / samples
            val decay = exp(-t * 4.0)
            val noise = rng.nextDouble() * 2.0 - 1.0
            val shimmer = sin(2 * PI * 800 * i / SAMPLE_RATE) * 0.2
            pcmClamp((noise + shimmer) * decay * 0.75)
        }
    }

    private fun generateRide(): ShortArray {
        val samples = msToSamples(80)
        return ShortArray(samples) { i ->
            val t = i.toDouble() / samples
            val decay = exp(-t * 8.0)
            val tone = sin(2 * PI * 350 * i / SAMPLE_RATE)
            val overtone = sin(2 * PI * 700 * i / SAMPLE_RATE) * 0.3
            pcmClamp((tone + overtone) * decay * 0.8)
        }
    }

    private fun generateBass(): ShortArray {
        val samples = msToSamples(200)
        return ShortArray(samples) { i ->
            val t = i.toDouble() / samples
            val decay = exp(-t * 4.0)
            val pitch = 55.0 * (1.0 + (1.0 - t) * 0.8)
            val tone = sin(2 * PI * pitch * i / SAMPLE_RATE)
            val sub = sin(2 * PI * 40 * i / SAMPLE_RATE) * 0.4
            pcmClamp((tone + sub) * decay * 0.95)
        }
    }

    private fun writePcm(dir: File, name: String, pcm: ShortArray): File {
        val file = File(dir, name)
        val dataBytes = pcm.size * 2
        FileOutputStream(file).use { fos ->
            DataOutputStream(fos).use { dos ->
                writeWavHeader(dos, dataBytes)
                for (sample in pcm) {
                    // Little-endian 16-bit
                    dos.writeByte(sample.toInt() and 0xFF)
                    dos.writeByte((sample.toInt() shr 8) and 0xFF)
                }
            }
        }
        return file
    }

    private fun writeWavHeader(dos: DataOutputStream, dataBytes: Int) {
        val byteRate = SAMPLE_RATE * 2        // mono 16-bit
        val totalSize = 36 + dataBytes

        dos.writeBytes("RIFF")
        writeLEInt(dos, totalSize)
        dos.writeBytes("WAVE")
        dos.writeBytes("fmt ")
        writeLEInt(dos, 16)                   // PCM chunk size
        writeLEShort(dos, 1)                  // PCM format
        writeLEShort(dos, 1)                  // mono
        writeLEInt(dos, SAMPLE_RATE)
        writeLEInt(dos, byteRate)
        writeLEShort(dos, 2)                  // block align
        writeLEShort(dos, 16)                 // bits per sample
        dos.writeBytes("data")
        writeLEInt(dos, dataBytes)
    }

    private fun writeLEInt(dos: DataOutputStream, value: Int) {
        dos.writeByte(value and 0xFF)
        dos.writeByte((value shr 8) and 0xFF)
        dos.writeByte((value shr 16) and 0xFF)
        dos.writeByte((value shr 24) and 0xFF)
    }

    private fun writeLEShort(dos: DataOutputStream, value: Int) {
        dos.writeByte(value and 0xFF)
        dos.writeByte((value shr 8) and 0xFF)
    }

    private fun msToSamples(ms: Int) = SAMPLE_RATE * ms / 1000

    private fun pcmClamp(v: Double): Short =
        (v * Short.MAX_VALUE).toLong()
            .coerceIn(Short.MIN_VALUE.toLong(), Short.MAX_VALUE.toLong())
            .toShort()
}
