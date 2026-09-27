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

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AccountActivity : AppCompatActivity() {

    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContentView(R.layout.activity_account)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()

        findViewById<android.view.View>(R.id.btnBackAccount).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnLogoutAccount).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
            sharedPreferences.edit { clear() }
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupUI() {
        val tvName = findViewById<TextView>(R.id.tvAccountName)
        val tvEmail = findViewById<TextView>(R.id.tvAccountEmail)
        val tvPoints = findViewById<TextView>(R.id.tvAccountPoints)
        val tvContributions = findViewById<TextView>(R.id.tvAccountContributions)
        val tvPhone = findViewById<TextView>(R.id.tvAccountPhone)

        currentUserId?.let { uid ->
            val db = FirebaseFirestore.getInstance()
            db.collection("usuarios").document(uid).get()
                .addOnSuccessListener { doc ->
                    val username = doc.getString("usuario") ?: "Usuario"
                    tvName.text = username
                    tvEmail.text = FirebaseAuth.getInstance().currentUser?.email
                    tvPoints.text = (doc.getLong("puntos") ?: 0).toString()
                    tvPhone.text = doc.getString("telefono") ?: "No registrado"

                    // Conteo de aportes
                    db.collection("prendas")
                        .whereEqualTo("vendedorId", uid)
                        .get()
                        .addOnSuccessListener { result ->
                            tvContributions.text = result.size().toString()
                        }
                }
        }

        // Funcionalidad "Mi Actividad"
        findViewById<android.view.View>(R.id.btnHistorialCanjes).setOnClickListener {
            val intent = Intent(this, ExchangesActivity::class.java)
            intent.putExtra("SELECT_TAB", 1) // 1 es la pestaña de "Mis Canjes"
            startActivity(intent)
        }

        findViewById<android.view.View>(R.id.btnMisPublicaciones).setOnClickListener {
            val intent = Intent(this, ExchangesActivity::class.java)
            intent.putExtra("SELECT_TAB", 0) // 0 es la pestaña de "Mis Aportes"
            startActivity(intent)
        }
    }
}