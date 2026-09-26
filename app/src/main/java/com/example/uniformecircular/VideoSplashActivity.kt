package com.example.uniformecircular

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.VideoView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import kotlin.math.max

class VideoSplashActivity : AppCompatActivity() {
    private var isVideoReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Instalar el Splash y configurar pantalla completa total
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Permitir que el contenido ignore los límites de las barras y el notch
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.attributes.layoutInDisplayCutoutMode = 
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        // 2. Transición fluida
        splashScreen.setOnExitAnimationListener { it.remove() }
        splashScreen.setKeepOnScreenCondition { !isVideoReady }

        setContentView(R.layout.activity_video_splash)
        hideSystemUI()

        val videoView = findViewById<VideoView>(R.id.videoView)
        val videoPath = "android.resource://" + packageName + "/" + R.raw.splash_video
        val uri = Uri.parse(videoPath)
        
        videoView.setVideoURI(uri)

        videoView.setOnPreparedListener { mp ->
            // 3. Lógica Center Crop: Llenar TODA la pantalla sin deformar
            val videoWidth = mp.videoWidth.toFloat()
            val videoHeight = mp.videoHeight.toFloat()
            
            // Usamos las dimensiones del contenedor (FrameLayout) que es match_parent
            val parent = videoView.parent as View
            val containerWidth = parent.width.toFloat()
            val containerHeight = parent.height.toFloat()

            if (videoWidth > 0 && videoHeight > 0 && containerWidth > 0 && containerHeight > 0) {
                // Dimensiones actuales del VideoView (que ya ajustó su aspect ratio en onMeasure)
                val viewWidth = videoView.width.toFloat()
                val viewHeight = videoView.height.toFloat()

                // Calculamos cuánto necesitamos escalar para llenar el contenedor
                val scaleX = containerWidth / viewWidth
                val scaleY = containerHeight / viewHeight

                // Usamos el factor máximo para lograr el efecto "Center Crop" (llenar y recortar excedentes)
                val finalScale = max(scaleX, scaleY)
                
                videoView.scaleX = finalScale
                videoView.scaleY = finalScale
            }

            videoView.start()

            // 4. Sincronización para evitar el parpadeo blanco
            mp.setOnInfoListener { _, what, _ ->
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    isVideoReady = true
                }
                true
            }
            
            // Fallback por seguridad
            videoView.postDelayed({ isVideoReady = true }, 20)
        }

        videoView.setOnCompletionListener {
            startActivity(Intent(this, Login::class.java))
            finish()
        }
    }

    private fun hideSystemUI() {
        window.insetsController?.let {
            it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

