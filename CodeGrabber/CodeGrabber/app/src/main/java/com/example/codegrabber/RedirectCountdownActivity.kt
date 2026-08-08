package com.example.codegrabber

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Full-screen countdown shown right after a code is copied.
 * Mimics a "shutting down" style bar that depletes over a few seconds.
 * Tapping "Open Now" (or letting the timer run out) opens the redeem site
 * and auto-pastes the code. "Close" cancels the redirect.
 */
class RedirectCountdownActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CODE = "extra_code"
        const val EXTRA_URL = "extra_url"
        private const val COUNTDOWN_MS = 6000L // 6 seconds, adjust as you like
    }

    private var animator: ValueAnimator? = null
    private var redirected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over the lock screen / on top of whatever app is open, like a system alert.
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContentView(R.layout.activity_redirect_countdown)

        val code = intent.getStringExtra(EXTRA_CODE) ?: ""
        val url = intent.getStringExtra(EXTRA_URL) ?: "https://reward.ff.garena.com/en"

        val label = findViewById<TextView>(R.id.redirectLabel)
        val progressBar = findViewById<ProgressBar>(R.id.depletingBar)
        val openNowButton = findViewById<Button>(R.id.openNowButton)
        val closeButton = findViewById<Button>(R.id.closeButton)

        label.text = "Code copied: $code\nRedirecting to reward.ff.garena.com..."

        progressBar.max = 1000
        progressBar.progress = 1000

        animator = ValueAnimator.ofInt(1000, 0).apply {
            duration = COUNTDOWN_MS
            addUpdateListener {
                progressBar.progress = it.animatedValue as Int
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (!redirected) openSite(code, url)
                }
            })
            start()
        }

        openNowButton.setOnClickListener {
            animator?.cancel()
            openSite(code, url)
        }

        closeButton.setOnClickListener {
            animator?.cancel()
            finish()
        }
    }

    private fun openSite(code: String, url: String) {
        if (redirected) return
        redirected = true
        val intent = Intent(this, RedeemWebViewActivity::class.java).apply {
            putExtra(RedeemWebViewActivity.EXTRA_CODE, code)
            putExtra(RedeemWebViewActivity.EXTRA_URL, url)
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        animator?.cancel()
    }
}
