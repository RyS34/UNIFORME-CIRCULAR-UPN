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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeliverActivity : AppCompatActivity() {
    
    private lateinit var dbHelper: DBHelper
    private var currentUserId: Int = -1
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private var editPrendaId: Int = -1

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

        dbHelper = DBHelper(this)

        // Obtener ID del usuario actual
        val username = intent.getStringExtra("USER_NAME") ?: ""
        currentUserId = dbHelper.obtenerUsuarioId(username)

        // Verificar si estamos en modo edición
        editPrendaId = intent.getIntExtra("EDIT_PRENDA_ID", -1)

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

        if (editPrendaId != -1) {
            cargarDatosPrenda(editPrendaId)
        }
    }
    // Carga los datos de la prenda a editar en la UI de edición de prenda
    private fun cargarDatosPrenda(idPrenda: Int) {
        // Cambiar textos de la UI para modo edición
        findViewById<TextView>(R.id.tvTitle).text = "Editar Publicación"
        findViewById<Button>(R.id.btnSubirPrenda).text = "Guardar Cambios"

        val cursor = dbHelper.readableDatabase.query(
            DBHelper.TABLE_PRENDAS, null, "${DBHelper.COL_PRENDA_ID} = ?", 
            arrayOf(idPrenda.toString()), null, null, null
        )

        if (cursor.moveToFirst()) {
            val titulo = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_TITULO))
            val descripcion = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_DESCRIPCION))
            val carrera = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_CARRERA))
            val talla = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_TALLA))
            val genero = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_GENERO))
            val tipo = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_TIPO))
            val imagenUriStr = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_IMAGEN))
            val estadoFisico = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_ESTADO_FISICO))

            findViewById<TextInputEditText>(R.id.etTitulo).setText(titulo)
            findViewById<TextInputEditText>(R.id.etDescripcion).setText(descripcion)

            // Spinner Carrera
            val spinner = findViewById<Spinner>(R.id.spCarrera)
            val adapter = spinner.adapter as? ArrayAdapter<String>
            val carreraPos = adapter?.getPosition(carrera) ?: -1
            if (carreraPos >= 0) spinner.setSelection(carreraPos)

            // Chips Talla
            val cgTalla = findViewById<ChipGroup>(R.id.cgTalla)
            for (i in 0 until cgTalla.childCount) {
                val chip = cgTalla.getChildAt(i) as Chip
                if (chip.text.toString() == talla) {
                    chip.isChecked = true
                    break
                }
            }

            // Chips Genero
            val cgGenero = findViewById<ChipGroup>(R.id.cgGenero)
            for (i in 0 until cgGenero.childCount) {
                val chip = cgGenero.getChildAt(i) as Chip
                if (chip.text.toString() == genero) {
                    chip.isChecked = true
                    break
                }
            }

            // Chips Estado Físico
            val cgEstado = findViewById<ChipGroup>(R.id.cgEstadoFisico)
            for (i in 0 until cgEstado.childCount) {
                val chip = cgEstado.getChildAt(i) as Chip
                if (chip.text.toString() == estadoFisico) {
                    chip.isChecked = true
                    break
                }
            }

            // Chips Tipo
            val cgTipo = findViewById<ChipGroup>(R.id.cgTipo)
            for (i in 0 until cgTipo.childCount) {
                val chip = cgTipo.getChildAt(i) as Chip
                if (chip.text.toString() == tipo) {
                    chip.isChecked = true
                    break
                }
            }

            // Imagen
            if (!imagenUriStr.isNullOrEmpty()) {
                val uri = Uri.parse(imagenUriStr)
                actualizarPreview(uri)
            }
        }
        cursor.close()
        actualizarResumenPuntos()
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

        val puntosBaseCondicion = when (selectedEstadoId) {
            R.id.chipNuevo -> 20
            R.id.chipSeminuevo -> 10
            R.id.chipUsado -> 0
            else -> 0
        }

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

        tvPuntos.text = "$puntosCalculados pts"
    }
    // Muestra la imagen de la prenda a pantalla completa
    private fun showFullImage(uri: Uri) {
        val fullImageDialog = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val view = layoutInflater.inflate(R.layout.dialog_full_image, null)
        fullImageDialog.setContentView(view)
        
        val ivFull = fullImageDialog.findViewById<ImageView>(R.id.ivFullImage)
        val btnClose = fullImageDialog.findViewById<ImageButton>(R.id.btnCloseFullImage)
        
        ivFull.setImageURI(uri)
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

        if (currentUserId == -1) {
            Toast.makeText(this, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        // Guardar la imagen localmente en el almacenamiento interno para evitar la pérdida de permisos de la Uri de la galería
        val rutaImagenLocal = guardarImagenEnAlmacenamientoInterno(selectedImageUri!!)
        if (rutaImagenLocal == null) {
            Toast.makeText(this, "Error al procesar la imagen seleccionada", Toast.LENGTH_SHORT).show()
            return
        }

        // --- LÓGICA DE PUNTOS DINÁMICA ---
        // Puntos base por condición física
        val puntosBaseCondicion = when (selectedEstadoId) {
            R.id.chipNuevo -> 20
            R.id.chipSeminuevo -> 10
            else -> 0 // chipUsado
        }

        // Puntos por modalidad (Base)
        // Guardamos valores internos consistentes en la DB: "Intercambio", "Donación", "Venta"
        val (tipoDB, puntosBaseModalidad) = when (selectedTipoId) {
            R.id.chipDonacion -> "Donación" to 0
            R.id.chipVenta -> "Venta" to 30
            else -> "Intercambio" to 50
        }

        // Si es donación, siempre es 0 puntos independientemente del estado
        val puntosCalculados = if (tipoDB == "Donación") 0 else puntosBaseModalidad + puntosBaseCondicion

        // Guardamos la URI persistente de la imagen local como String
        val exito = if (editPrendaId == -1) {
            dbHelper.insertarPrenda(
                currentUserId,
                titulo,
                descripcion,
                carrera,
                talla,
                genero,
                tipoDB,
                puntos = puntosCalculados,
                imagen = rutaImagenLocal,
                estadoFisico = estadoFisico
            )
        } else {
            dbHelper.actualizarPrenda(
                editPrendaId,
                titulo,
                descripcion,
                carrera,
                talla,
                genero,
                tipoDB,
                puntos = puntosCalculados,
                imagen = rutaImagenLocal,
                estadoFisico = estadoFisico
            )
        }

        if (exito) {
            val mensaje = if (editPrendaId == -1) 
                "¡Prenda publicada! Ganarás los puntos cuando confirmes la entrega física." 
            else "Publicación actualizada correctamente."
            
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            finish()
        } else {
            Toast.makeText(this, "Error al procesar la prenda", Toast.LENGTH_SHORT).show()
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
