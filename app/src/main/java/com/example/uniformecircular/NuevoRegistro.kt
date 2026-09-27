package com.example.uniformecircular

import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.View

class NuevoRegistro : AppCompatActivity() {

    private lateinit var tilNombre: TextInputLayout
    private lateinit var tilUsuario: TextInputLayout
    private lateinit var tilTelefono: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilRepPassword: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nuevo_registro)

        // Ajustar insets para que el fondo sea total
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainRegistro)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            findViewById<View>(R.id.linearLayoutTop).setPadding(0, systemBars.top, 0, 0)
            findViewById<View>(R.id.linearLayoutBottom).setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        val etNombre = findViewById<EditText>(R.id.nombre)
        val etUsuario = findViewById<EditText>(R.id.usuario)
        val etTelefono = findViewById<EditText>(R.id.telefono)
        val etPassword = findViewById<TextInputEditText>(R.id.password)
        val etRepPassword = findViewById<TextInputEditText>(R.id.rep_password)
        
        tilNombre = findViewById(R.id.nombreInputLayout)
        tilUsuario = findViewById(R.id.usuarioInputLayout)
        tilTelefono = findViewById(R.id.phoneInputLayout)
        tilPassword = findViewById(R.id.passwordInputLayout)
        tilRepPassword = findViewById(R.id.repPasswordInputLayout)
        
        val tvSubtitulo = findViewById<android.widget.TextView>(R.id.subtituloRegistro)
        val btnRegistrarUsuario = findViewById<Button>(R.id.btnRegistrarUsuario)
        val btnVolverLogin = findViewById<Button>(R.id.btnVolverLogin)

        // Limpiar errores al escribir y validar espacios en tiempo real
        etNombre.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tilNombre.error = null
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        etUsuario.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val userText = s?.toString() ?: ""
                
                // MEJORA DINÁMICA: Cambiar subtítulo si es admin
                if (userText.trim().lowercase() == "admin") {
                    tvSubtitulo.text = getString(R.string.admin_info_sub)
                    tvSubtitulo.setTextColor(getColor(R.color.brand_blue))
                } else {
                    tvSubtitulo.text = getString(R.string.welcome_bonus_info)
                    tvSubtitulo.setTextColor(getColor(R.color.brand_blue))
                }

                if (userText.contains(" ")) {
                    tilUsuario.error = getString(R.string.error_username_no_spaces)
                } else {
                    tilUsuario.error = null
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        etTelefono.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tilTelefono.error = null
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        etPassword.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val pass = s?.toString() ?: ""
                when {
                    pass.isEmpty() -> tilPassword.error = null
                    pass.contains(" ") -> tilPassword.error = getString(R.string.error_password_no_spaces)
                    pass.length < 8 -> tilPassword.error = getString(R.string.error_password_too_short)
                    else -> tilPassword.error = null
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        etRepPassword.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tilRepPassword.error = null
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        btnRegistrarUsuario.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val user = etUsuario.text.toString() // Sin trim para detectar espacios en validación
            val phone = etTelefono.text.toString().trim()
            val pass = etPassword.text.toString()
            val repPass = etRepPassword.text.toString()

            var isValid = true

            if (nombre.isEmpty()) {
                tilNombre.error = getString(R.string.error_name_empty)
                isValid = false
            } else if (!nombre.contains(" ")) {
                tilNombre.error = getString(R.string.error_name_invalid)
                isValid = false
            }

            // Validar espacios en el usuario
            if (user.contains(" ")) {
                tilUsuario.error = getString(R.string.error_username_no_spaces)
                etUsuario.requestFocus()
                isValid = false
            } else if (user.isEmpty()) {
                tilUsuario.error = getString(R.string.error_all_fields_required)
                isValid = false
            }

            if (phone.isEmpty()) {
                tilTelefono.error = getString(R.string.error_all_fields_required)
                isValid = false
            } else if (phone.length < 9) {
                tilTelefono.error = "Número de teléfono inválido"
                isValid = false
            }

            if (pass.isEmpty()) {
                tilPassword.error = getString(R.string.error_all_fields_required)
                isValid = false
            } else if (pass.length < 8) {
                tilPassword.error = getString(R.string.error_password_too_short)
                isValid = false
            } else if (pass.contains(" ")) {
                tilPassword.error = getString(R.string.error_password_no_spaces)
                isValid = false
            }

            if (repPass != pass) {
                tilRepPassword.error = getString(R.string.error_passwords_dont_match)
                isValid = false
            }

            if (!isValid) return@setOnClickListener

            // --- LÓGICA DE FIREBASE ---
            val mAuth = FirebaseAuth.getInstance()
            val db = FirebaseFirestore.getInstance()
            
            val userClean = user.trim().replace("\\s".toRegex(), "").lowercase()
            val emailReal = if (userClean.contains("@")) userClean else "$userClean@upn.pe"

            // 1. Crear usuario en Auth con el correo real
            mAuth.createUserWithEmailAndPassword(emailReal, pass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = mAuth.currentUser?.uid
                        val cleanUsernameForDB = userClean.split("@")[0].uppercase()
                        val isAdminUser = cleanUsernameForDB.lowercase() == "admin"
                        
                        val datosUsuario = hashMapOf(
                            "nombre" to nombre,
                            "usuario" to cleanUsernameForDB,
                            "telefono" to phone,
                            "puntos" to if (isAdminUser) 0 else 100, // Bono de bienvenida: 100 puntos para usuarios normales, 0 para admin
                            "rol" to if (isAdminUser) "ADMIN" else "USER"
                        )

                        // 2. Guardar datos adicionales en Firestore
                        if (userId != null) {
                            db.collection("usuarios").document(userId)
                                .set(datosUsuario)
                                .addOnSuccessListener {
                                    if (isAdminUser) {
                                        Toast.makeText(this, "¡Administrador registrado en la nube!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this, "¡Registro exitoso! Recibiste 100 puntos.", Toast.LENGTH_SHORT).show()
                                    }
                                    regresarAlLogin()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        val errorMsg = task.exception?.message ?: "Error desconocido"
                        if (errorMsg.contains("already in use")) {
                            tilUsuario.error = "El nombre de usuario ya existe"
                        } else {
                            Toast.makeText(this, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }

        btnVolverLogin.setOnClickListener { regresarAlLogin() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { regresarAlLogin() }
        })
    }

    private fun regresarAlLogin() {
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val logoView = findViewById<View>(R.id.imgLogoRegistro)
        if (logoView != null) {
            val options = ActivityOptions.makeSceneTransitionAnimation(this, logoView, "logo_shared")
            startActivity(intent, options.toBundle())
        } else {
            startActivity(intent)
        }
        finish()
    }
}