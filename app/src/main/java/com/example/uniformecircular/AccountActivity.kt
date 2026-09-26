package com.example.uniformecircular

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import androidx.core.content.edit

class AccountActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContentView(R.layout.activity_account)

        dbHelper = DBHelper(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val username = intent.getStringExtra("USER_NAME") ?: "Usuario"
        currentUserId = dbHelper.obtenerUsuarioId(username)

        setupUI(username)

        findViewById<android.view.View>(R.id.btnBackAccount).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnLogoutAccount).setOnClickListener {
            val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
            sharedPreferences.edit { clear() }
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupUI(username: String) {
        val tvName = findViewById<TextView>(R.id.tvAccountName)
        val tvEmail = findViewById<TextView>(R.id.tvAccountEmail)
        val tvPoints = findViewById<TextView>(R.id.tvAccountPoints)
        val tvContributions = findViewById<TextView>(R.id.tvAccountContributions)
        val tvPhone = findViewById<TextView>(R.id.tvAccountPhone)

        tvName.text = username
        val emailHandle = username.trim().lowercase().replace(" ", "")
        tvEmail.text = getString(R.string.user_email_template, emailHandle)
        
        if (currentUserId != -1) {
            tvPoints.text = dbHelper.obtenerPuntosUsuario(currentUserId).toString()
            tvContributions.text = dbHelper.obtenerConteoAportes(currentUserId).toString()
            tvPhone.text = dbHelper.obtenerTelefonoUsuario(currentUserId) ?: "No registrado"
        }

        // Funcionalidad "Mi Actividad"
        findViewById<android.view.View>(R.id.btnHistorialCanjes).setOnClickListener {
            val intent = Intent(this, ExchangesActivity::class.java)
            intent.putExtra("USER_NAME", username)
            intent.putExtra("SELECT_TAB", 1) // 1 es la pestaña de "Mis Canjes"
            startActivity(intent)
        }

        findViewById<android.view.View>(R.id.btnMisPublicaciones).setOnClickListener {
            val intent = Intent(this, ExchangesActivity::class.java)
            intent.putExtra("USER_NAME", username)
            intent.putExtra("SELECT_TAB", 0) // 0 es la pestaña de "Mis Aportes"
            startActivity(intent)
        }
    }
}