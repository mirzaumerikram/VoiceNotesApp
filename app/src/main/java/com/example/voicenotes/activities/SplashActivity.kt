package com.example.voicenotes.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.voicenotes.R
import com.example.voicenotes.databinding.ActivitySplashBinding
import com.example.voicenotes.utils.SessionManager

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val anim = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        binding.ivMicLogo.startAnimation(anim)

        Handler(Looper.getMainLooper()).postDelayed({
            val session = SessionManager(this)
            val next = if (session.isLoggedIn()) HomeActivity::class.java else LoginActivity::class.java
            startActivity(Intent(this, next))
            finish()
        }, 2000)
    }
}
