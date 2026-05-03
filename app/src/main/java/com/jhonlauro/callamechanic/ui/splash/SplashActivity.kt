package com.jhonlauro.callamechanic.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jhonlauro.callamechanic.MainActivity
import com.jhonlauro.callamechanic.databinding.ActivitySplashBinding
import com.jhonlauro.callamechanic.ui.common.AppTransitions

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.logoCard.alpha = 0f
        binding.logoCard.scaleX = 0.72f
        binding.logoCard.scaleY = 0.72f
        binding.tvAppName.alpha = 0f
        binding.tvAppName.translationY = 24f
        animateBackgroundSymbols()

        binding.logoCard.animate()
            .alpha(1f)
            .scaleX(1.08f)
            .scaleY(1.08f)
            .setDuration(340)
            .withEndAction {
                binding.logoCard.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start()
                binding.tvAppName.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(260)
                    .withEndAction {
                        binding.root.postDelayed({
                            startActivity(Intent(this, MainActivity::class.java))
                            AppTransitions.fade(this)
                            finish()
                        }, 180)
                    }
                    .start()
            }
            .start()
    }

    private fun animateBackgroundSymbols() {
        listOf(
            binding.bgCarTop,
            binding.bgWrenchTop,
            binding.bgBoltLeft,
            binding.bgShieldRight,
            binding.bgClockBottom,
            binding.bgCarBottom
        ).forEachIndexed { index, view ->
            view.scaleX = 0.72f
            view.scaleY = 0.72f
            view.alpha = 0f
            view.translationY = 18f
            view.animate()
                .alpha(if (index % 2 == 0) 0.13f else 0.11f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setStartDelay((index * 55).toLong())
                .setDuration(360)
                .start()
        }
    }
}
