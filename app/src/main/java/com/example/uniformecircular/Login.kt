package com.example.uniformecircular

import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.content.edit

class Login : AppCompatActivity() {

    private lateinit var btnNuevoRegistro: Button
    private lateinit var btnLogin: Button
    private lateinit var etUsuario: EditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var tilUsuario: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var cbRememberMe: CheckBox
    private lateinit var tvForgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)


        // ===== AJUSTES DE INSETS =====
        // Ajustar insets para que el fondo sea total
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Aplicamos el padding a los layouts internos, no a la raíz que tiene el fondo
            findViewById<View>(R.id.linearLayoutTop).setPadding(0, systemBars.top, 0, 0)
            findViewById<View>(R.id.linearLayoutBottom).setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        // ===== VINCULACIÓN DE COMPONENTES DE LA INTERFAZ (UI) =====
        etUsuario = findViewById(R.id.usuario)
        etPassword = findViewById(R.id.password)
        tilUsuario = findViewById(R.id.usuarioInputLayout)
        tilPassword = findViewById(R.id.passwordInputLayout)
        cbRememberMe = findViewById(R.id.cbRememberMe)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        btnLogin = findViewById(R.id.loginButton)
        btnNuevoRegistro = findViewById(R.id.RegistrarButton)

        // Restaurar datos guardados y estado de la casilla "Recordarme" para inicio automático
        val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        val savedUser = sharedPreferences.getString("saved_username", "")
        val savedPass = sharedPreferences.getString("saved_password", "")
        val isRemembered = sharedPreferences.getBoolean("remember_me", false)

        if (isRemembered && !savedUser.isNullOrEmpty() && !savedPass.isNullOrEmpty()) {
            etUsuario.setText(savedUser)
            etPassword.setText(savedPass)
            cbRememberMe.isChecked = true

            // Validar con Firebase de forma transparente
            val emailFicticio = "$savedUser@upn.pe"
            FirebaseAuth.getInstance().signInWithEmailAndPassword(emailFicticio, savedPass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val intent = Intent(this, MainActivity::class.java).apply {
                            putExtra("USER_NAME", savedUser)
                        }
                        val options = ActivityOptions.makeSceneTransitionAnimation(this, findViewById(R.id.imgLogoContainer), "logo_shared")
                        startActivity(intent, options.toBundle())
                        finish()
                    }
                }
            return
        } else if (isRemembered && !savedUser.isNullOrEmpty()) {
            etUsuario.setText(savedUser)
            cbRememberMe.isChecked = true
        }

        // Limpiar errores al escribir para una mejor experiencia de usuario
        etUsuario.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val userText = s?.toString() ?: ""
                if (userText.contains(" ")) {
                    tilUsuario.error = getString(R.string.error_username_no_spaces)
                } else {
                    tilUsuario.error = null
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        // Limpiar errores al escribir para una mejor experiencia de usuario
        etPassword.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val pass = s?.toString() ?: ""
                when {
                    pass.isEmpty() -> {
                        tilPassword.error = null
                    }
                    pass.contains(" ") -> {
                        tilPassword.error = getString(R.string.error_password_no_spaces)
                    }
                    pass.length < 8 -> {
                        tilPassword.error = getString(R.string.error_password_too_short)
                    }
                    else -> {
                        tilPassword.error = null
                    }
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Ejecutar login al presionar "Listo" en el teclado (Punto 1: ImeOptions)
        etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                btnLogin.performClick()
                true
            } else {
                false
            }
        }
        //Pulsación botón login
        btnLogin.setOnClickListener {
            val user = etUsuario.text.toString() // Sin trim para validación exacta
            val pass = etPassword.text.toString() // Sin trim para ser precisos con la nueva política de espacios

            // Validar campos vacíos con feedback inmediato
            var isValid = true
            if (user.trim().isEmpty()) {
                tilUsuario.error = getString(R.string.error_empty_username)
                isValid = false
            }
            if (pass.isEmpty()) {
                tilPassword.error = getString(R.string.error_empty_password)
                isValid = false
            }

            if (!isValid) {
                if (user.isEmpty()) etUsuario.requestFocus()
                else etPassword.requestFocus()
                return@setOnClickListener
            }

            // Validar espacios en el usuario
            if (user.contains(" ")) {
                tilUsuario.error = getString(R.string.error_username_no_spaces)
                etUsuario.requestFocus()
                return@setOnClickListener
            }

            // Validaciones de formato antes de consultar la DB (UX)
            if (pass.length < 8) {
                tilPassword.error = getString(R.string.error_password_too_short)
                etPassword.requestFocus()
                return@setOnClickListener
            }
            // Validar espacios en la contraseña
            if (pass.contains(" ")) {
                tilPassword.error = getString(R.string.error_password_no_spaces)
                etPassword.requestFocus()
                return@setOnClickListener
            }

            // --- LÓGICA DE LOGIN CON FIREBASE ---
            val mAuth = FirebaseAuth.getInstance()
            val emailFicticio = "${user.trim()}@upn.pe"

            mAuth.signInWithEmailAndPassword(emailFicticio, pass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Guardar o limpiar preferencia de recordar
                        sharedPreferences.edit {
                            if (cbRememberMe.isChecked) {
                                putString("saved_username", user)
                                putString("saved_password", pass)
                                putBoolean("remember_me", true)
                            } else {
                                clear()
                            }
                        }

                        // Ir a la pantalla principal
                        val intent = Intent(this, MainActivity::class.java).apply {
                            putExtra("USER_NAME", user)
                        }
                        val options = ActivityOptions.makeSceneTransitionAnimation(this, findViewById(R.id.imgLogoContainer), "logo_shared")
                        startActivity(intent, options.toBundle())
                        finish()
                    } else {
                        val errorMsg = task.exception?.message ?: ""
                        // Capturar errores comunes de Firebase Auth en inglés y traducirlos de forma clara
                        if (errorMsg.contains("user-not-found") || 
                            errorMsg.contains("There is no user record")) {
                            Toast.makeText(this, "El usuario ingresado no existe", Toast.LENGTH_SHORT).show()
                        } else if (errorMsg.contains("wrong-password") || 
                                   errorMsg.contains("invalid-credential")) {
                            Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                        } else if (errorMsg.contains("network-request-failed")) {
                            Toast.makeText(this, "Error de red: Verifica tu conexión a internet", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }
        //Pulsación botón nuevo registro
        btnNuevoRegistro.setOnClickListener {
            val intent = Intent(this, NuevoRegistro::class.java)
            val options = ActivityOptions.makeSceneTransitionAnimation(this, findViewById(R.id.imgLogoContainer), "logo_shared")
            startActivity(intent, options.toBundle())
        }

        // --- LÓGICA DE RECUPERAR CONTRASEÑA ---
        tvForgotPassword.setOnClickListener {
            mostrarDialogoRecuperacion()
        }
    }
    // Mostrar diálogo de recuperación de contraseña al presionar el texto
    private fun mostrarDialogoRecuperacion() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_recuperar_clave, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        // Obtener elementos del diálogo y configurar listeners
        val etUser = dialogView.findViewById<TextInputEditText>(R.id.etUserRecup)
        val tilUser = dialogView.findViewById<TextInputLayout>(R.id.tilUserRecup)
        val btnValidar = dialogView.findViewById<Button>(R.id.btnValidarRecuperacion)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelarRecuperacion)
        
        // Ocultar campo de teléfono, ya que Firebase usa el correo (usuario)
        dialogView.findViewById<TextInputLayout>(R.id.tilPhoneRecup).visibility = View.GONE

        btnCancelar.setOnClickListener { dialog.dismiss() }

        btnValidar.setOnClickListener {
            val user = etUser.text.toString().trim()

            if (user.isEmpty()) {
                tilUser.error = "Ingresa tu usuario"
                return@setOnClickListener
            }
            // Validar espacios en el usuario ingresado
            val emailFicticio = "$user@upn.pe"
            FirebaseAuth.getInstance().sendPasswordResetEmail(emailFicticio)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Se envió un enlace de recuperación a tu correo institucional ($emailFicticio)", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                    } else {
                        val errorMsg = task.exception?.message ?: ""
                        if (errorMsg.contains("user-not-found") || errorMsg.contains("invalid-credential") || errorMsg.contains("There is no user record")) {
                            Toast.makeText(this, "El usuario ingresado no existe", Toast.LENGTH_SHORT).show()
                        } else if (errorMsg.contains("network-request-failed")) {
                            Toast.makeText(this, "Error de red: Verifica tu conexión a internet", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "No se pudo enviar el correo de recuperación", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }

        dialog.show()
    }
    // Restaurar datos guardados y estado de la casilla "Recordarme" al volver a la pantalla
    override fun onResume() {
        super.onResume()
        val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        val isRemembered = sharedPreferences.getBoolean("remember_me", false)
        
        if (!isRemembered) {
            etUsuario.setText("")
        }
        
        etPassword.setText("")
        tilUsuario.error = null
        tilPassword.error = null
        
        // Enfocar el campo vacío correspondiente
        if (etUsuario.text.toString().isEmpty()) {
            etUsuario.requestFocus()
        } else {
            etPassword.requestFocus()
        }
    }
}
