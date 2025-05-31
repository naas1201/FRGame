package com.votrenom.francerraison // Adaptez IMPÉRATIVEMENT à votre structure de package exacte

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen")
class StartupActivity : AppCompatActivity() {

    private val SPLASH_DELAY_MS: Long = 3500
    private val FADE_IN_DURATION_MS: Long = 1200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Cette ligne lie le fichier XML de layout à cette activité Kotlin.
        // Assurez-vous que res/layout/activity_startup.xml existe et est correct.
        setContentView(R.layout.activity_startup)

        // Récupération des vues depuis le layout XML par leurs IDs
        // Assurez-vous que ces IDs existent dans activity_startup.xml
        val logoView: ImageView = findViewById(R.id.imageViewLogo)
        val studioNameView: TextView = findViewById(R.id.textViewStudioName)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)

        // Animations de fondu
        val logoFadeIn = AlphaAnimation(0.0f, 1.0f).apply {
            duration = FADE_IN_DURATION_MS
            fillAfter = true
        }

        val textFadeIn = AlphaAnimation(0.0f, 1.0f).apply {
            duration = FADE_IN_DURATION_MS
            startOffset = 300
            fillAfter = true
        }

        val progressFadeIn = AlphaAnimation(0.0f, 1.0f).apply {
            duration = FADE_IN_DURATION_MS
            startOffset = 600
            fillAfter = true
        }

        // Démarrage des animations
        logoView.startAnimation(logoFadeIn)
        studioNameView.startAnimation(textFadeIn)
        progressBar.startAnimation(progressFadeIn)

        // Handler pour retarder la transition vers MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            // Assurez-vous que MainActivity existe et est déclarée dans AndroidManifest.xml
            val mainIntent = Intent(this@StartupActivity, MainActivity::class.java)
            startActivity(mainIntent)
            finish() // Termine StartupActivity
        }, SPLASH_DELAY_MS)
    }
}
