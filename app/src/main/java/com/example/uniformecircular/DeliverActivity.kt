package com.example.uniformecircular

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.bumptech.glide.Glide

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream

class DeliverActivity : AppCompatActivity() {
    
    private var currentUserId: String? = null
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private var editPrendaId: String? = null

    // Selector de imágenes de la galería
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            actualizarPreview(uri)
        }
    }

    // Captura de foto con la cámara
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraImageUri?.let { actualizarPreview(it) }
        }
    }
    // Actualiza la vista previa de la imagen seleccionada
    private fun actualizarPreview(uri: Uri) {
        selectedImageUri = uri
        val ivPreview = findViewById<ImageView>(R.id.ivPrendaPreview)
        ivPreview.setImageURI(uri)
        ivPreview.scaleType = ImageView.ScaleType.CENTER_CROP
        // Usamos post para asegurar que el drawable ya se haya cargado antes de quitar el tinte
        ivPreview.post {
            ivPreview.drawable?.setTintList(null)
        }
    }
    // Configuración de la actividad de entrega
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deliver)

        // Obtener ID del usuario actual desde Firebase
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        // Verificar si estamos en modo edición (ahora es un String de Firestore)
        editPrendaId = intent.getStringExtra("EDIT_PRENDA_ID")

        setupInsets()
        setupSpinner()
        
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        findViewById<Button>(R.id.btnSeleccionarFoto).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        findViewById<Button>(R.id.btnTomarFoto).setOnClickListener {
            prepararCamara()
        }

        findViewById<Button>(R.id.btnSubirPrenda).setOnClickListener {
            validarYSubir()
        }

        // Limpiar errores al escribir
        findViewById<TextInputEditText>(R.id.etTitulo).doOnTextChanged { _, _, _, _ ->
            findViewById<TextInputLayout>(R.id.tilTitulo).error = null
        }
        findViewById<TextInputEditText>(R.id.etDescripcion).doOnTextChanged { _, _, _, _ ->
            findViewById<TextInputLayout>(R.id.tilDescripcion).error = null
        }

        // Permitir ver la imagen en grande al hacer clic en la vista previa
        findViewById<ImageView>(R.id.ivPrendaPreview).setOnClickListener {
            selectedImageUri?.let { uri ->
                showFullImage(uri)
            }
        }

        // Configurar listeners para actualización de puntos en tiempo real
        setupPointsListeners()

        if (editPrendaId != null) {
            cargarDatosPrenda(editPrendaId!!)
        }
    }
    // Carga los datos de la prenda a editar desde Firestore
    private fun cargarDatosPrenda(idPrenda: String) {
        findViewById<TextView>(R.id.tvTitle).text = "Editar Publicación"
        findViewById<Button>(R.id.btnSubirPrenda).text = "Guardar Cambios"

        FirebaseFirestore.getInstance().collection("prendas").document(idPrenda)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    findViewById<TextInputEditText>(R.id.etTitulo).setText(doc.getString("titulo"))
                    findViewById<TextInputEditText>(R.id.etDescripcion).setText(doc.getString("descripcion"))
                    
                    val carrera = doc.getString("carrera")
                    val spinner = findViewById<Spinner>(R.id.spCarrera)
                    val adapter = spinner.adapter as? ArrayAdapter<String>
                    val carreraPos = adapter?.getPosition(carrera) ?: -1
                    if (carreraPos >= 0) spinner.setSelection(carreraPos)

                    val imagenUrl = doc.getString("urlImagen")
                    if (!imagenUrl.isNullOrEmpty()) {
                        val ivPreview = findViewById<ImageView>(R.id.ivPrendaPreview)
                        
                        if (imagenUrl.startsWith("base64:")) {
                            try {
                                val base64String = imagenUrl.substring(7)
                                val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                                val decodedImage = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                ivPreview.setImageBitmap(decodedImage)
                                ivPreview.scaleType = ImageView.ScaleType.CENTER_CROP
                                ivPreview.post { ivPreview.drawable?.setTintList(null) }
                            } catch (e: Exception) {
                                ivPreview.setImageResource(android.R.drawable.ic_menu_gallery)
                            }
                        } else {
                            Glide.with(this@DeliverActivity)
                                .load(imagenUrl)
                                .centerCrop()
                                .into(ivPreview)
                            ivPreview.post { ivPreview.drawable?.setTintList(null) }
                        }
                        
                        // Guardamos la URI para que la validación sepa que ya hay una imagen
                        selectedImageUri = android.net.Uri.parse(imagenUrl)
                    }
                    
                    // Cargar Chips
                    val talla = doc.getString("talla")
                    val genero = doc.getString("genero")
                    val tipo = doc.getString("tipo")
                    val estadoFisico = doc.getString("estadoFisico")

                    seleccionarChipPorTexto(findViewById(R.id.cgTalla), talla)
                    seleccionarChipPorTexto(findViewById(R.id.cgGenero), genero)
                    seleccionarChipPorTexto(findViewById(R.id.cgTipo), tipo)
                    seleccionarChipPorTexto(findViewById(R.id.cgEstadoFisico), estadoFisico)

                    actualizarResumenPuntos()
                }
            }
    }

    private fun seleccionarChipPorTexto(group: ChipGroup, texto: String?) {
        if (texto == null) return
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as? Chip
            if (chip?.text?.toString().equals(texto, ignoreCase = true)) {
                chip?.isChecked = true
                break
            }
        }
    }
    // Actualiza el resumen de puntos en tiempo real al cambiar el estado físico o la modalidad
    private fun setupPointsListeners() {
        val cgEstadoFisico = findViewById<ChipGroup>(R.id.cgEstadoFisico)
        val cgTipo = findViewById<ChipGroup>(R.id.cgTipo)

        cgEstadoFisico.setOnCheckedChangeListener { _, _ -> actualizarResumenPuntos() }
        cgTipo.setOnCheckedChangeListener { _, _ -> actualizarResumenPuntos() }

        // Inicializar el valor
        actualizarResumenPuntos()
    }
    // Actualiza el resumen de puntos en tiempo real al cambiar el estado físico o la modalidad
    private fun actualizarResumenPuntos() {
        val selectedEstadoId = findViewById<ChipGroup>(R.id.cgEstadoFisico).checkedChipId
        val selectedTipoId = findViewById<ChipGroup>(R.id.cgTipo).checkedChipId
        val tvPuntos = findViewById<TextView>(R.id.tvPuntosCalculados)

        // Calculamos los puntos según el estado físico seleccionados (nuevo, seminuevo, usado)
        val puntosBaseCondicion = when (selectedEstadoId) {
            R.id.chipNuevo -> 20
            R.id.chipSeminuevo -> 10
            R.id.chipUsado -> 0
            else -> 0
        }
        // Calculamos los puntos según la modalidad seleccionada (intercambio, venta, donación)
        val puntosBaseModalidad = when (selectedTipoId) {
            R.id.chipIntercambio -> 50
            R.id.chipVenta -> 30
            R.id.chipDonacion -> 0
            else -> 0
        }

        // Si la modalidad seleccionada es Donación, los puntos son siempre 0.
        // Si no hay modalidad seleccionada, pero sí estado físico, mostramos el bono del estado.
        val puntosCalculados = if (selectedTipoId == R.id.chipDonacion) {
            0
        } else {
            puntosBaseModalidad + puntosBaseCondicion
        }
        // Actualizamos el resumen de puntos en tiempo real
        tvPuntos.text = "$puntosCalculados pts"
    }
    // Muestra la imagen de la prenda a pantalla completa
    private fun showFullImage(uri: Uri) {
        val fullImageDialog = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val view = layoutInflater.inflate(R.layout.dialog_full_image, null)
        fullImageDialog.setContentView(view)
        
        val ivFull = fullImageDialog.findViewById<ImageView>(R.id.ivFullImage)
        val btnClose = fullImageDialog.findViewById<ImageButton>(R.id.btnCloseFullImage)
        
        val uriString = uri.toString()
        if (uriString.startsWith("base64:")) {
            try {
                val base64String = uriString.substring(7)
                val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                val decodedImage = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ivFull.setImageBitmap(decodedImage)
            } catch (e: Exception) {
                ivFull.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } else {
            ivFull.setImageURI(uri)
        }

        btnClose.setOnClickListener { fullImageDialog.dismiss() }
        
        fullImageDialog.show()
    }
    // Prepara la cámara para capturar una foto nueva y la muestra en la vista previa
    private fun prepararCamara() {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val imageFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
            
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                imageFile
            )
            cameraImageUri = uri
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al preparar la cámara", Toast.LENGTH_SHORT).show()
        }
    }
    // Configura los insets para el diseño Edge-to-Edge en una actividad de entrega
    private fun setupInsets() {
        val rootView = findViewById<View>(R.id.mainDeliver)
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
    }
    // Configura el Spinner de carreras en la actividad de entrega
    private fun setupSpinner() {
        val spinner = findViewById<Spinner>(R.id.spCarrera)
        val carreras = listOf("Ingeniería", "Salud", "Derecho", "Arquitectura", "Negocios", "Comunicaciones")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, carreras)
        spinner.adapter = adapter
    }
    // Válida y sube una nueva prenda a la base de datos y muestra un mensaje de éxito o error
        private fun validarYSubir() {
        val tilTitulo = findViewById<TextInputLayout>(R.id.tilTitulo)
        val etTitulo = findViewById<TextInputEditText>(R.id.etTitulo)
        val tilDescripcion = findViewById<TextInputLayout>(R.id.tilDescripcion)
        val etDescripcion = findViewById<TextInputEditText>(R.id.etDescripcion)
        val cvFoto = findViewById<MaterialCardView>(R.id.cvFotoPrenda)

        val titulo = etTitulo.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()
        val carrera = findViewById<Spinner>(R.id.spCarrera).selectedItem.toString()

        // Validaciones Visuales
        var hasError = false

        if (selectedImageUri == null) {
            cvFoto.strokeColor = ContextCompat.getColor(this, android.R.color.holo_red_dark)
            Toast.makeText(this, getString(R.string.error_empty_photo), Toast.LENGTH_SHORT).show()
            hasError = true
        } else {
            cvFoto.strokeColor = ContextCompat.getColor(this, R.color.slate_100)
        }

        if (descripcion.isEmpty()) {
            tilDescripcion.error = getString(R.string.error_empty_desc)
            etDescripcion.requestFocus()
            hasError = true
        }

        if (titulo.isEmpty()) {
            tilTitulo.error = getString(R.string.error_empty_title)
            etTitulo.requestFocus()
            hasError = true
        }

        if (hasError) return

        val cgTalla = findViewById<ChipGroup>(R.id.cgTalla)
        val selectedTallaId = cgTalla.checkedChipId
        if (selectedTallaId == View.NO_ID) {
            Toast.makeText(this, getString(R.string.error_empty_talla), Toast.LENGTH_SHORT).show()
            return
        }
        val talla = findViewById<Chip>(selectedTallaId).text.toString()

        val cgGenero = findViewById<ChipGroup>(R.id.cgGenero)
        val selectedGeneroId = cgGenero.checkedChipId
        if (selectedGeneroId == View.NO_ID) {
            Toast.makeText(this, getString(R.string.error_empty_gender), Toast.LENGTH_SHORT).show()
            return
        }
        val genero = findViewById<Chip>(selectedGeneroId).text.toString()

        val cgEstadoFisico = findViewById<ChipGroup>(R.id.cgEstadoFisico)
        val selectedEstadoId = cgEstadoFisico.checkedChipId
        if (selectedEstadoId == View.NO_ID) {
            Toast.makeText(this, getString(R.string.error_empty_condition), Toast.LENGTH_SHORT).show()
            return
        }
        val estadoFisico = findViewById<Chip>(selectedEstadoId).text.toString()

        val cgTipo = findViewById<ChipGroup>(R.id.cgTipo)
        val selectedTipoId = cgTipo.checkedChipId
        if (selectedTipoId == View.NO_ID) {
            Toast.makeText(this, getString(R.string.error_empty_modality), Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUserId == null) {
            Toast.makeText(this, "Error: Inicia sesión nuevamente", Toast.LENGTH_SHORT).show()
            return
        }

        // --- MIGRACIÓN A FIREBASE (Versión sin Storage - Base64) ---
        val btnSubir = findViewById<Button>(R.id.btnSubirPrenda)
        btnSubir.isEnabled = false
        btnSubir.text = "Subiendo..."

        // Convertimos la imagen seleccionada a Base64 comprimido y corregimos orientación
        val imagenBase64 = selectedImageUri?.let { uri ->
            val uriString = uri.toString()
            if (uriString.startsWith("base64:")) {
                return@let uriString
            }

            try {
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                
                // Corregir la orientación EXIF
                val exifInputStream = contentResolver.openInputStream(uri)
                val exif = exifInputStream?.let { ExifInterface(it) }
                val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }
                
                val correctedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
                
                val outputStream = java.io.ByteArrayOutputStream()
                // Comprimimos al 40% para que quepa en Firestore y no use mucha red
                correctedBitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
                val byteArray = outputStream.toByteArray()
                "base64:" + android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
            } catch (e: Exception) {
                null
            }
        }

        // Si falló la conversión, usamos la de repuesto
        val urlFinal = imagenBase64 ?: "https://images.unsplash.com/photo-1523381210434-271e8be1f52b?q=80&w=500"

        // Preparar datos para Firestore
        val (tipoDB, puntosBaseModalidad) = when (selectedTipoId) {
            R.id.chipDonacion -> "Donación" to 0
            R.id.chipVenta -> "Venta" to 30
            else -> "Intercambio" to 50
        }
        val puntosCalculados = if (tipoDB == "Donación") 0 else puntosBaseModalidad + when (selectedEstadoId) {
            R.id.chipNuevo -> 20
            R.id.chipSeminuevo -> 10
            else -> 0
        }

        val prendaData = hashMapOf(
            "vendedorId" to currentUserId,
            "titulo" to titulo,
            "descripcion" to descripcion,
            "carrera" to carrera,
            "talla" to talla,
            "genero" to genero,
            "tipo" to tipoDB,
            "puntos" to puntosCalculados,
            "urlImagen" to urlFinal,
            "estadoFisico" to estadoFisico,
            "estadoPublicacion" to "DISPONIBLE",
            "fechaCreacion" to com.google.firebase.Timestamp.now()
        )

        // Guardar directamente en Firestore
        val firestore = FirebaseFirestore.getInstance()
        val docRef = if (editPrendaId == null) {
            firestore.collection("prendas").document()
        } else {
            firestore.collection("prendas").document(editPrendaId!!)
        }

        docRef.set(prendaData)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Publicado con TU foto con éxito!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener {
                btnSubir.isEnabled = true
                btnSubir.text = "Intentar de nuevo"
                Toast.makeText(this, "Error Firestore: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
    // Guarda la imagen seleccionada en el almacenamiento interno de la aplicación y devuelve su URI
    private fun guardarImagenEnAlmacenamientoInterno(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = "prenda_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)
            val outputStream = java.io.FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
