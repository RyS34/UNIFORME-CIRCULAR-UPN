package com.example.uniformecircular

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        // Ajustar insets para diseño Edge-to-Edge inmersivo usando el espaciador dinámico
        val rootView = findViewById<View>(R.id.mainHelp)
        val statusBarSpacer = findViewById<View>(R.id.statusBarSpacer)
        val content = findViewById<View>(R.id.layoutContent)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            statusBarSpacer.layoutParams.height = systemBars.top
            statusBarSpacer.requestLayout()

            content.setPadding(
                content.paddingLeft,
                content.paddingTop,
                content.paddingRight,
                systemBars.bottom
            )
            insets
        }

        // Animación de entrada para el contenido
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        content.startAnimation(fadeIn)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        findViewById<MaterialButton>(R.id.btnSupport).setOnClickListener {
            showSupportOptions()
        }

        setupFaqToggles()
    }
    // Configura los toggles de FAQ
    private fun setupFaqToggles() {
        val faqs = listOf(
            Triple(R.id.cardFaq1, R.id.tvAnswer1, R.id.ivArrow1),
            Triple(R.id.cardFaq2, R.id.tvAnswer2, R.id.ivArrow2),
            Triple(R.id.cardFaq3, R.id.tvAnswer3, R.id.ivArrow3)
        )

        faqs.forEach { (cardId, answerId, arrowId) ->
            val card = findViewById<View>(cardId)
            val answer = findViewById<View>(answerId)
            val arrow = findViewById<View>(arrowId)

            card.setOnClickListener {
                if (answer.visibility == View.VISIBLE) {
                    answer.visibility = View.GONE
                    arrow.animate().rotation(0f).setDuration(300).start()
                } else {
                    answer.visibility = View.VISIBLE
                    arrow.animate().rotation(180f).setDuration(300).start()
                }
            }
        }
    }
    // Muestra opciones de soporte personalizadas usando el diseño inflado
    private fun showSupportOptions() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_support_options, null)
        
        val alertDialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Configurar los clics en los elementos del diseño personalizado
        dialogView.findViewById<View>(R.id.btnWhatsAppOption).setOnClickListener {
            alertDialog.dismiss()
            contactViaWhatsApp()
        }

        dialogView.findViewById<View>(R.id.btnEmailOption).setOnClickListener {
            alertDialog.dismiss()
            contactViaEmail()
        }

        dialogView.findViewById<View>(R.id.btnCancelDialog).setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }
    // Implementa las funciones de contacto aquí
    private fun contactViaWhatsApp() {
        val phoneNumber = "+51927672725" // Reemplazar con el número real de soporte
        val message = "Hola, necesito soporte con la app Uniforme Circular."
        val url = "https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}"
        
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp no está instalado", Toast.LENGTH_SHORT).show()
        }
    }
    // Implementa las funciones de contacto aquí
    private fun contactViaEmail() {
        val email = "soporte@uniformecircular.com"
        val subject = "Soporte - Uniforme Circular"
        
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        
        try {
            startActivity(Intent.createChooser(intent, "Enviar correo..."))
        } catch (e: Exception) {
            Toast.makeText(this, "No se encontró una aplicación de correo", Toast.LENGTH_SHORT).show()
        }
    }
}

