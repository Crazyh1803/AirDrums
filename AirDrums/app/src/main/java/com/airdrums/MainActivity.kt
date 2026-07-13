package com.airdrums

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.airdrums.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var engine: DrumEngine
    private lateinit var haptic: HapticManager
    private lateinit var accel: AccelerometerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        engine = DrumEngine(this)
        haptic = HapticManager(this)

        accel = AccelerometerManager(this) {
            engine.hit(DrumEngine.Drum.BASS)
            haptic.bassHit()
            binding.drumPadView.triggerBassFlash()
            flashBassIndicator()
        }

        binding.drumPadView.onDrumHit = { drum ->
            engine.hit(drum)
            haptic.padHit()
        }

        // Sensitivity slider: maps 0-100 → threshold 8-30 m/s²
        binding.sensitivitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                accel.threshold = 8f + (progress / 100f) * 22f
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        // Show loading overlay; hide it when all sounds are ready
        binding.loadingText.visibility = View.VISIBLE
        binding.drumPadView.alpha = 0.3f

        engine.onAllLoaded = {
            runOnUiThread {
                binding.loadingText.visibility = View.GONE
                binding.drumPadView.animate().alpha(1f).setDuration(300).start()
            }
        }

        Thread {
            engine.loadSounds(cacheDir)
        }.start()
    }

    private fun flashBassIndicator() {
        binding.bassIndicator.visibility = View.VISIBLE
        binding.bassIndicator.animate().cancel()
        binding.bassIndicator.alpha = 1f
        binding.bassIndicator.animate()
            .alpha(0f)
            .setDuration(400)
            .withEndAction { binding.bassIndicator.visibility = View.INVISIBLE }
            .start()
    }

    override fun onResume() {
        super.onResume()
        accel.register()
    }

    override fun onPause() {
        super.onPause()
        accel.unregister()
    }

    override fun onDestroy() {
        super.onDestroy()
        engine.release()
    }
}
