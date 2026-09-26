package com.example.uniformecircular

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

import android.view.animation.AnimationUtils

class CatalogActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper
    private lateinit var adapter: ProductAdapter
    private var allProducts: List<Prenda> = listOf()
    
    private var selectedCarrera: String = "Todas"
    private var selectedTalla: String = "Todas"
    private var selectedGenero: String = "Todos"
    private var searchText: String = ""

    private var currentUserId: Int = -1

    // Configuración inicial de la actividad
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalog)

        dbHelper = DBHelper(this)
        val userName = intent.getStringExtra("USER_NAME") ?: "Usuario"
        currentUserId = dbHelper.obtenerUsuarioId(userName)

        // Ajustar insets para diseño Edge-to-Edge inmersivo usando el espaciador dinámico
        val rootView = findViewById<View>(R.id.mainCatalog)
        val statusBarSpacer = findViewById<View>(R.id.statusBarSpacer)
        val layoutContent = findViewById<View>(R.id.rvCatalog)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            statusBarSpacer.layoutParams.height = systemBars.top
            statusBarSpacer.requestLayout()

            layoutContent.setPadding(
                layoutContent.paddingLeft,
                layoutContent.paddingTop,
                layoutContent.paddingRight,
                systemBars.bottom
            )
            insets
        }
        // Inicializamos la base de datos
        dbHelper = DBHelper(this)

        // Sincronizar número de admin por si cambió en el código
        val miNumeroReal = "927672725"
        if (dbHelper.existeUsuario("admin")) {
            dbHelper.actualizarTelefonoUsuario("admin", miNumeroReal)
        }

        // Botón Atrás
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        // Configuramos el RecyclerView
        setupRecyclerView()
        setupFilterChips()
        setupSearch()
        setupInfoButton()

        // Manejar término de búsqueda si viene del Dashboard
        val initialQuery = intent.getStringExtra("SEARCH_QUERY")
        if (!initialQuery.isNullOrEmpty()) {
            val etSearch = findViewById<EditText>(R.id.etSearch)
            etSearch.setText(initialQuery)
            searchText = initialQuery
        }

        // Manejar categoría seleccionada desde el Dashboard
        val initialCategory = intent.getStringExtra("SELECTED_CATEGORY")
        if (!initialCategory.isNullOrEmpty()) {
            selectedCarrera = initialCategory
            // El Chip se marcará automáticamente en setupFilterChips si coincide con el texto
        }

        // Manejar el foco de búsqueda o producto específico desde el Dashboard
        val targetPrendaId = intent.getIntExtra("PRENDA_ID", -1)
        if (targetPrendaId != -1) {
            abrirDetallePrendaPorId(targetPrendaId)
        } else if (intent.getBooleanExtra("FOCUS_SEARCH", false)) {
            val etSearch = findViewById<EditText>(R.id.etSearch)
            etSearch.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    // Abrimos el detalle de un producto por su ID (si existe)
    private fun abrirDetallePrendaPorId(id: Int) {
        // Buscamos en la lista cargada (asegurándonos de que ya se cargó en onResume o loadProducts)
        val prenda = allProducts.find { it.id == id }
        prenda?.let { showProductDetail(it) }
    }

    // Cargamos los productos desde la base de datos cada vez que la pantalla vuelve a estar en primer plano (onResume)
    override fun onResume() {
        super.onResume()
        // Cargamos o recargamos los productos desde la base de datos cada vez que la pantalla vuelve a estar en primer plano
        loadProducts()
    }
    // Configuramos el botón de información
    private fun setupInfoButton() {
        findViewById<ImageButton>(R.id.btnInfoPoints).setOnClickListener {
            val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
            val view = layoutInflater.inflate(R.layout.dialog_points_info, null)
            
            view.findViewById<Button>(R.id.btnPointsClose).setOnClickListener {
                dialog.dismiss()
            }
            
            dialog.setContentView(view)
            
            // Forzar que el diálogo se abra completo (Expandido)
            dialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            dialog.behavior.skipCollapsed = true

            dialog.show()
        }
    }
    // Configuramos el RecyclerView
    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.rvCatalog)
        val columns = resources.getInteger(R.integer.grid_columns)
        recyclerView.layoutManager = GridLayoutManager(this, columns)
        
        // Animación de cascada para el catálogo
        val animation = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fall_down)
        recyclerView.layoutAnimation = animation

        adapter = ProductAdapter(allProducts, isCompact = false) { prenda ->
            showProductDetail(prenda)
        }
        recyclerView.adapter = adapter
    }
    // Mostramos el detalle del producto en un BottomSheetDialog
    @SuppressLint("DiscouragedApi", "InflateParams")
    private fun showProductDetail(prenda: Prenda) {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.dialog_product_detail, null, false)
        
        // Vincular vistas del diálogo
        val ivImage = view.findViewById<ImageView>(R.id.ivDetailImage)
        val tvTitle = view.findViewById<TextView>(R.id.tvDetailPrendaName)
        val tvPoints = view.findViewById<TextView>(R.id.tvDetailPoints)
        val tvCategory = view.findViewById<TextView>(R.id.tvDetailCategory)
        val tvOwner = view.findViewById<TextView>(R.id.tvDetailOwner)
        val tvType = view.findViewById<TextView>(R.id.tvDetailType)
        val tvEstadoFisico = view.findViewById<TextView>(R.id.tvDetailEstadoFisico)
        val tvDescription = view.findViewById<TextView>(R.id.tvDetailDescription)
        val btnContact = view.findViewById<Button>(R.id.btnContact)
        val btnAdminMenu = view.findViewById<ImageButton>(R.id.btnAdminMenu)
        val btnCloseDetail = view.findViewById<ImageButton>(R.id.btnCloseDetail)

        // Asignar datos básicos
        tvTitle.text = prenda.titulo
        tvPoints.text = getString(R.string.label_points, prenda.puntos)
        tvCategory.text = getString(R.string.label_category_talla_genero, prenda.carrera, prenda.talla, prenda.genero)
        
        val ownerName = dbHelper.obtenerNombreUsuario(prenda.idUsuario)
        tvOwner.text = ownerName ?: "Estudiante"

        tvType.text = prenda.tipoTransaccion
        tvEstadoFisico.text = prenda.estadoFisico
        tvDescription.text = prenda.descripcion

        // Mostrar Alerta de Observación en el Detalle si aplica (Solo para el Admin)
        val layoutAdminObs = view.findViewById<View>(R.id.layoutAdminObservationAlert)
        val tvAdminObsReason = view.findViewById<TextView>(R.id.tvAdminObservationReason)

        if (prenda.estado == "OBSERVADA") {
            layoutAdminObs.visibility = View.VISIBLE
            tvAdminObsReason.text = prenda.observacion ?: "Sin motivo"
        } else {
            layoutAdminObs.visibility = View.GONE
        }

        // Configuración del botón de contacto y menú de admin
        val loggedInUsername = dbHelper.obtenerNombreUsuario(currentUserId) ?: ""
        val isAdmin = loggedInUsername.trim().lowercase() == "admin"

        if (prenda.idUsuario == currentUserId) {
            btnContact.visibility = View.GONE
        } else if (isAdmin) {
            btnContact.text = getString(R.string.btn_contact_admin)
            // Se mantiene el diseño visual del botón (color e icono) para un acabado Premium y consistente

            // MOSTRAR MENÚ DE ADMINISTRACIÓN MODERNO
            btnAdminMenu.visibility = View.VISIBLE
            btnAdminMenu.setOnClickListener {
                val optionsDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
                val optionsView = layoutInflater.inflate(R.layout.dialog_admin_options, null)
                
                val optObserve = optionsView.findViewById<View>(R.id.optionObserve)
                val optDelete = optionsView.findViewById<View>(R.id.optionDelete)

                optObserve.setOnClickListener {
                    optionsDialog.dismiss()
                    // Mostrar diálogo de observación (ya modernizado)
                    val dialogView = layoutInflater.inflate(R.layout.dialog_admin_observe, null)
                    val customDialog = androidx.appcompat.app.AlertDialog.Builder(this)
                        .setView(dialogView)
                        .create()
                    
                    customDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                    
                    val etMotivo = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etMotivo)
                    val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
                    val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)

                    btnCancel.setOnClickListener { customDialog.dismiss() }
                    btnConfirm.setOnClickListener {
                        val motivo = etMotivo.text.toString().trim()
                        if (motivo.isNotEmpty()) {
                            if (dbHelper.observarPrenda(prenda.id, motivo)) {
                                Toast.makeText(this@CatalogActivity, "OBSERVACIÓN ENVIADA", Toast.LENGTH_SHORT).show()
                                loadProducts()
                                customDialog.dismiss()
                                dialog.dismiss()
                            }
                        } else {
                            etMotivo.error = getString(R.string.error_empty_observation)
                        }
                    }
                    customDialog.show()
                }

                optDelete.setOnClickListener {
                    optionsDialog.dismiss()
                    // Mostrar diálogo de eliminación (ya modernizado)
                    val dialogView = layoutInflater.inflate(R.layout.dialog_admin_delete, null)
                    val customDialog = androidx.appcompat.app.AlertDialog.Builder(this)
                        .setView(dialogView)
                        .create()
                    
                    customDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                    
                    val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
                    val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)

                    btnCancel.setOnClickListener { customDialog.dismiss() }
                    btnConfirm.setOnClickListener {
                        if (dbHelper.eliminarPrenda(prenda.id)) {
                            loadProducts()
                            customDialog.dismiss()
                            dialog.dismiss()
                        }
                    }
                    customDialog.show()
                }

                optionsDialog.setContentView(optionsView)
                optionsDialog.show()
            }
        } else {
            btnContact.text = getString(R.string.btn_contact)
        }

        // Configurar botón cerrar
        btnCloseDetail.setOnClickListener {
            dialog.dismiss()
        }
        
        // Cargar imagen en el detalle de forma dinámica (soporta tanto imágenes subidas como drawables por defecto)
        val imageUriString = prenda.imagenUri
        if (imageUriString != null && (imageUriString.startsWith("content://") || imageUriString.startsWith("file://"))) {
            ivImage.setImageURI(android.net.Uri.parse(imageUriString))
        } else {
            val imageName = imageUriString?.trim()?.lowercase()
            val resId = when(imageName) {
                "chompa_upn" -> R.drawable.chompa_upn
                "pantalon_upn" -> R.drawable.pantalon_upn
                "bata_upn" -> R.drawable.bata_upn
                "casaca_deportiva_upn" -> R.drawable.casaca_deportiva_upn
                else -> if (!imageName.isNullOrEmpty()) {
                    resources.getIdentifier(imageName, "drawable", packageName)
                } else 0
            }

            if (resId != 0) {
                ivImage.setImageResource(resId)
            } else {
                ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }

        // Permitir ver la imagen en grande al hacer clic
        ivImage.setOnClickListener {
            showFullImage(prenda)
        }

        dialog.setContentView(view)

        // Configurar el comportamiento del BottomSheet para que se expanda por completo al abrirse
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(it)
            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }

        // Botón de contacto al hacer clic
        btnContact.setOnClickListener {
            val telefono = dbHelper.obtenerTelefonoUsuario(prenda.idUsuario)
            if (!telefono.isNullOrEmpty()) {
                try {
                    val currentUsername = intent.getStringExtra("USER_NAME") ?: "Usuario"
                    val currentUserId = dbHelper.obtenerUsuarioId(currentUsername)
                    val loggedInUser = dbHelper.obtenerNombreUsuario(currentUserId) ?: ""
                    val isAdmin = loggedInUser.trim().lowercase() == "admin"

                    val mensaje: String
                    
                    if (isAdmin) {
                        // Flujo ADMIN: Cambiar estado a OBSERVADA para ocultar del catálogo y enviar mensaje
                        mensaje = getString(R.string.admin_contact_message, prenda.titulo)
                        dbHelper.observarPrenda(prenda.id, "Publicación bajo supervisión del administrador")
                        loadProducts()
                    } else {
                        // Flujo ESTUDIANTE: Reserva de prenda y gestión de puntos
                        if (prenda.idUsuario != currentUserId) {
                            // Verificar si el usuario tiene puntos suficientes para el intercambio
                            val puntosUsuario = dbHelper.obtenerPuntosUsuario(currentUserId)
                            if (puntosUsuario < prenda.puntos) {
                                Toast.makeText(this@CatalogActivity, getString(R.string.error_insufficient_points, prenda.puntos), Toast.LENGTH_SHORT).show()
                                return@setOnClickListener
                            }
                            // Reservamos la prenda en la base de datos
                            val db = dbHelper.writableDatabase
                            val values = android.content.ContentValues().apply {
                                put(DBHelper.COL_PRENDA_ESTADO, "EN PROCESO")
                                put(DBHelper.COL_PRENDA_ID_RECEPTOR, currentUserId)
                            }
                            db.update(DBHelper.TABLE_PRENDAS, values, "${DBHelper.COL_PRENDA_ID} = ?", arrayOf(prenda.id.toString()))
                            
                            // Reservamos los puntos del receptor (los restamos ahora)
                            dbHelper.restarPuntosUsuario(currentUserId, prenda.puntos)
                        }
                        mensaje = getString(R.string.contact_message_template, prenda.titulo)
                    }

                    // Intentamos con la URI nativa de WhatsApp primero
                    val whatsappUriUri = android.net.Uri.parse("whatsapp://send?phone=51$telefono&text=${android.net.Uri.encode(mensaje)}")
                    val intentNative = android.content.Intent(android.content.Intent.ACTION_VIEW, whatsappUriUri)
                    
                    try {
                        startActivity(intentNative)
                    } catch (eNative: Exception) {
                        // Segundo intento: Enlace web tradicional api.whatsapp.com
                        val webUrl = "https://api.whatsapp.com/send?phone=51$telefono&text=${android.net.Uri.encode(mensaje)}"
                        val intentWeb = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(webUrl))
                        try {
                            startActivity(intentWeb)
                        } catch (eWeb: Exception) {
                            // Tercer intento: Enlace wa.me
                            val waMeUrl = "https://wa.me/51$telefono?text=${android.net.Uri.encode(mensaje)}"
                            val intentWaMe = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(waMeUrl))
                            try {
                                startActivity(intentWaMe)
                            } catch (eWaMe: Exception) {
                                // Si todo falla, mostramos el error exacto en pantalla para diagnosticarlo perfectamente
                                Toast.makeText(
                                    this, 
                                    getString(R.string.error_whatsapp_failed, eWaMe.localizedMessage ?: eWaMe.toString()),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al procesar el contacto: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, getString(R.string.error_no_phone), Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.setContentView(view)
        
        // Forzar que el diálogo se abra completo (Expandido) y no se colapse a la mitad
        dialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true

        dialog.show()
    }

    // Muestra la imagen de la prenda a pantalla completa
    @SuppressLint("DiscouragedApi")
    private fun showFullImage(prenda: Prenda) {
        val fullImageDialog = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        fullImageDialog.setContentView(R.layout.dialog_full_image)
        
        val ivFull = fullImageDialog.findViewById<ImageView>(R.id.ivFullImage)
        val btnClose = fullImageDialog.findViewById<ImageButton>(R.id.btnCloseFullImage)
        
        val imageUriString = prenda.imagenUri
        if (imageUriString != null && (imageUriString.startsWith("content://") || imageUriString.startsWith("file://"))) {
            ivFull.setImageURI(android.net.Uri.parse(imageUriString))
        } else {
            val imageName = imageUriString?.trim()?.lowercase()
            val resId = when(imageName) {
                "chompa_upn" -> R.drawable.chompa_upn
                "pantalon_upn" -> R.drawable.pantalon_upn
                "bata_upn" -> R.drawable.bata_upn
                "casaca_deportiva_upn" -> R.drawable.casaca_deportiva_upn
                else -> if (!imageName.isNullOrEmpty()) {
                    resources.getIdentifier(imageName, "drawable", packageName)
                } else 0
            }
            if (resId != 0) ivFull.setImageResource(resId)
            else ivFull.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        // Botón de cierre al hacer clic
        btnClose.setOnClickListener { fullImageDialog.dismiss() }
        fullImageDialog.show()
    }

    // Configuramos los Chips para las carreras, tallas y géneros
    private fun setupFilterChips() {
        val cgCarreras = findViewById<ChipGroup>(R.id.cgCarreras)
        val cgTallas = findViewById<ChipGroup>(R.id.cgTallas)
        val cgGeneros = findViewById<ChipGroup>(R.id.cgGeneros)

        val carreras = listOf("Todas", "Ingeniería", "Salud", "Derecho", "Arquitectura", "Negocios", "Comunicaciones")
        val tallas = listOf("Todas", "XS", "S", "M", "L", "XL", "Única")
        val generos = listOf("Todos", "Caballero", "Dama", "Unisex")

        // Marcar las carreras seleccionadas inicialmente o "Todas" por defecto
        carreras.forEach { carrera ->
            val chip = Chip(this)
            chip.text = carrera
            chip.isCheckable = true
            chip.id = View.generateViewId()
            chip.setChipBackgroundColorResource(R.color.chip_background_selector)
            chip.setChipStrokeColorResource(R.color.chip_stroke_selector)
            chip.chipStrokeWidth = android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics
            )
            chip.setTextColor(androidx.core.content.ContextCompat.getColorStateList(this, R.color.chip_text_selector))
            if (carrera == selectedCarrera) {
                chip.isChecked = true
            }
            cgCarreras.addView(chip)
        }
        // Marcar las tallas seleccionadas inicialmente o "Todas" por defecto
        tallas.forEach { talla ->
            val chip = Chip(this)
            chip.text = talla
            chip.isCheckable = true
            chip.id = View.generateViewId()
            chip.setChipBackgroundColorResource(R.color.chip_background_selector)
            chip.setChipStrokeColorResource(R.color.chip_stroke_selector)
            chip.chipStrokeWidth = android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics
            )
            chip.setTextColor(androidx.core.content.ContextCompat.getColorStateList(this, R.color.chip_text_selector))
            if (talla == "Todas") chip.isChecked = true
            cgTallas.addView(chip)
        }
        // Marcar los géneros
        generos.forEach { genero ->
            val chip = Chip(this)
            chip.text = genero
            chip.isCheckable = true
            chip.id = View.generateViewId()
            chip.setChipBackgroundColorResource(R.color.chip_background_selector)
            chip.setChipStrokeColorResource(R.color.chip_stroke_selector)
            chip.chipStrokeWidth = android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics
            )
            chip.setTextColor(androidx.core.content.ContextCompat.getColorStateList(this, R.color.chip_text_selector))
            if (genero == "Todos") chip.isChecked = true
            cgGeneros.addView(chip)
        }
        // Actualizamos la selección de carrera al cambiar el chip
        cgCarreras.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            val chip = checkedId?.let { group.findViewById<Chip>(it) }
            selectedCarrera = chip?.text?.toString() ?: "Todas"
            applyFilters()
        }
        // Actualizamos la selección de talla al cambiar el chip
        cgTallas.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            val chip = checkedId?.let { group.findViewById<Chip>(it) }
            selectedTalla = chip?.text?.toString() ?: "Todas"
            applyFilters()
        }
        // Actualizamos la selección de género
        cgGeneros.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            val chip = checkedId?.let { group.findViewById<Chip>(it) }
            selectedGenero = chip?.text?.toString() ?: "Todos"
            applyFilters()
        }
    }
    // Cargamos los productos desde la base de datos
    private fun loadProducts() {
        val cursor = dbHelper.obtenerCatalogo()
        val products = mutableListOf<Prenda>()

        if (cursor.moveToFirst()) {
            do {
                val prenda = Prenda(
                    cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_ID)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_ID_USUARIO)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_TITULO)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_DESCRIPCION)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_CARRERA)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_TALLA)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_GENERO)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_TIPO)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_PUNTOS)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_IMAGEN)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_ESTADO)),
                    if (cursor.isNull(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_ID_RECEPTOR))) null
                    else cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_ID_RECEPTOR)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_ESTADO_FISICO)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_OBSERVACION))
                )
                // MEJORA: Solo mostramos en el catálogo público lo que está DISPONIBLE o EN PROCESO
                // Las prendas OBSERVADAS solo las verá el Administrador y el dueño en su perfil
                val loggedInUsername = dbHelper.obtenerNombreUsuario(currentUserId) ?: ""
                val isAdmin = loggedInUsername.trim().lowercase() == "admin"
                
                if (prenda.estado != "OBSERVADA" || isAdmin) {
                    products.add(prenda)
                }
            } while (cursor.moveToNext())
        }
        cursor.close()

        // Si la lista está vacía O si el primer producto no tiene imagen (datos viejos), refrescamos
        if (products.isEmpty() || products[0].imagenUri == null) {
            dbHelper.vaciarCatalogo() // Esta función la agregamos a DBHelper hace un momento
            insertSampleData()
            
            // Volvemos a cargar después de insertar
            val newCursor = dbHelper.obtenerCatalogo()
            val newProducts = mutableListOf<Prenda>()
            if (newCursor.moveToFirst()) {
                do {
                    val prenda = Prenda(
                        newCursor.getInt(newCursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_ID)),
                        newCursor.getInt(newCursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_ID_USUARIO)),
                        newCursor.getString(newCursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_TITULO)),
                        newCursor.getString(newCursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_DESCRIPCION)),
                        newCursor.getString(newCursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_CARRERA)),
                        newCursor.getString(newCursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_TALLA)),
                        newCursor.getString(newCursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_GENERO)),
                        newCursor.getString(newCursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_TIPO)),
                        newCursor.getInt(newCursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_PUNTOS)),
                        newCursor.getString(newCursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_IMAGEN)),
                        newCursor.getString(newCursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_ESTADO)),
                        if (newCursor.isNull(newCursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_ID_RECEPTOR))) null
                        else newCursor.getInt(newCursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_ID_RECEPTOR)),
                        newCursor.getString(newCursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_ESTADO_FISICO)),
                        newCursor.getString(newCursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_OBSERVACION))
                    )
                    newProducts.add(prenda)
                } while (newCursor.moveToNext())
            }
            newCursor.close()
            allProducts = newProducts
        } else {
            allProducts = products
        }

        applyFilters()
    }
    // Insertamos datos de prueba
    private fun insertSampleData() {
        val miNumeroReal = "927672725"
        
        // 1. Aseguramos que exista un usuario del sistema para ser dueño de las prendas de demostración
        if (!dbHelper.existeUsuario("UPN_Oficial")) {
            dbHelper.insertarUsuario("UPN_Oficial", "upn2024offic", miNumeroReal)
        }
        val systemUserId = dbHelper.obtenerUsuarioId("UPN_Oficial")

        // 2. Insertamos usando el ID del usuario del sistema y nombres exactos de archivos en drawable
        dbHelper.insertarPrenda(systemUserId, "Chompa UPN", "Chompa en buen estado", "Ingeniería", "M", "Unisex", "Intercambio", 50, "chompa_upn")
        dbHelper.insertarPrenda(systemUserId, "Pantalón de Tela", "Pantalón casi nuevo", "Derecho", "S", "Caballero", "Donación", 0, "pantalon_upn")
        dbHelper.insertarPrenda(systemUserId, "Bata de Laboratorio", "Bata blanca reglamentaria", "Salud", "L", "Unisex", "Venta", 30, "bata_upn")
        dbHelper.insertarPrenda(systemUserId, "Casaca Deportiva", "Casaca de la selección UPN", "Comunicaciones", "XL", "Unisex", "Intercambio", 80, "casaca_deportiva_upn")
    }
    // Configuramos el TextWatcher para la barra de búsqueda
    private fun setupSearch() {
        val etSearch = findViewById<EditText>(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchText = s.toString()
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    private fun String.removeAccents(): String {
        return java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase()
    }

    // Aplicamos los filtros y actualizamos la lista
    private fun applyFilters() {
        val normalizedQuery = searchText.removeAccents()
        
        val filteredList = allProducts.filter { prenda ->
            val matchesSearch = prenda.titulo.removeAccents().contains(normalizedQuery)
            val matchesCarrera = selectedCarrera == "Todas" || prenda.carrera.equals(selectedCarrera, ignoreCase = true)
            val matchesTalla = selectedTalla == "Todas" || prenda.talla.equals(selectedTalla, ignoreCase = true)
            val matchesGenero = selectedGenero == "Todos" || prenda.genero.equals(selectedGenero, ignoreCase = true)
            val isAvailable = prenda.estado == "DISPONIBLE"
            
            matchesSearch && matchesCarrera && matchesTalla && matchesGenero && isAvailable
        }
        // Actualizamos el adaptador con la lista filtrada
        adapter.updateList(filteredList)
        
        // Disparar animación de la cuadrícula
        val recyclerView = findViewById<RecyclerView>(R.id.rvCatalog)
        recyclerView.scheduleLayoutAnimation()
        
        val tvNoResults = findViewById<TextView>(R.id.tvNoResults)
        if (filteredList.isEmpty()) {
            tvNoResults.visibility = View.VISIBLE
        } else {
            tvNoResults.visibility = View.GONE
        }
    }
}
