package com.example.uniformecircular

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.net.Uri
import android.util.Base64
import android.graphics.BitmapFactory
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.content.Intent
import android.view.inputmethod.InputMethodManager
import android.app.Dialog
import android.util.TypedValue
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.uniformecircular.R
import android.view.animation.AnimationUtils
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class CatalogActivity : AppCompatActivity() {

    private lateinit var adapter: ProductAdapter
    private var allProducts: List<Prenda> = listOf()
    
    private var selectedCarrera: String = "Todas"
    private var selectedTalla: String = "Todas"
    private var selectedGenero: String = "Todos"
    private var searchText: String = ""

    private var currentUserId: String? = null
    private var currentRol: String = "USER"

    // Configuración inicial de la actividad
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalog)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        
        // Obtener rol del usuario actual desde Firestore
        currentUserId?.let { uid ->
            FirebaseFirestore.getInstance().collection("usuarios").document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    currentRol = doc.getString("rol") ?: "USER"
                    loadProducts()
                }
        }

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

        // Botón Atrás
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Botón Actualizar
        findViewById<ImageButton>(R.id.btnRefresh).setOnClickListener {
            loadProducts()
            Toast.makeText(this, "Catálogo actualizado", Toast.LENGTH_SHORT).show()
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
        val targetPrendaId = intent.getStringExtra("PRENDA_ID")
        if (targetPrendaId != null) {
            abrirDetallePrendaPorId(targetPrendaId)
        } else if (intent.getBooleanExtra("FOCUS_SEARCH", false)) {
            val etSearch = findViewById<EditText>(R.id.etSearch)
            etSearch.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    // Abrimos el detalle de un producto por su ID (si existe)
    private fun abrirDetallePrendaPorId(id: String) {
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
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
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
        val btnContact = view.findViewById<MaterialButton>(R.id.btnContact)
        val btnAdminMenu = view.findViewById<ImageButton>(R.id.btnAdminMenu)
        val btnCloseDetail = view.findViewById<ImageButton>(R.id.btnCloseDetail)

        // Asignar datos básicos
        tvTitle.text = prenda.titulo
        tvPoints.text = getString(R.string.label_points, prenda.puntos)
        tvCategory.text = getString(R.string.label_category_talla_genero, prenda.carrera, prenda.talla, prenda.genero)
        
        // Obtener nombre del dueño desde Firestore
        FirebaseFirestore.getInstance().collection("usuarios").document(prenda.idUsuario)
            .get()
            .addOnSuccessListener { doc ->
                tvOwner.text = doc.getString("nombre") ?: doc.getString("usuario") ?: "Estudiante"
            }

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
        val isAdmin = currentRol == "ADMIN"
        val isOwner = prenda.idUsuario == currentUserId

        if (isOwner) {
            btnContact.text = "EDITAR MI PUBLICACIÓN"
            btnContact.setIconResource(android.R.drawable.ic_menu_edit)
            btnContact.iconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
        } else if (isAdmin) {
            btnContact.text = getString(R.string.btn_contact_admin)
            btnContact.setIconResource(R.drawable.ic_whatsapp)
            btnContact.iconTint = null
            
            // MOSTRAR MENÚ DE ADMINISTRACIÓN MODERNO
            btnAdminMenu.visibility = View.VISIBLE
            btnAdminMenu.setOnClickListener {
                val optionsDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
                val optionsView = layoutInflater.inflate(R.layout.dialog_admin_options, null)
                
                val optObserve = optionsView.findViewById<View>(R.id.optionObserve)
                val optDelete = optionsView.findViewById<View>(R.id.optionDelete)

                optObserve.setOnClickListener {
                    optionsDialog.dismiss()
                    val dialogView = layoutInflater.inflate(R.layout.dialog_admin_observe, null)
                    val customDialog = androidx.appcompat.app.AlertDialog.Builder(this)
                        .setView(dialogView)
                        .create()
                    
                    customDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                    
                    val tilMotivo = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilMotivo)
                    val etMotivo = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etMotivo)
                    val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
                    val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)

                    etMotivo.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            tilMotivo.error = null
                        }
                        override fun afterTextChanged(s: Editable?) {}
                    })

                    btnCancel.setOnClickListener { customDialog.dismiss() }
                    btnConfirm.setOnClickListener {
                        val motivo = etMotivo.text.toString().trim()
                        if (motivo.isNotEmpty()) {
                            FirebaseFirestore.getInstance().collection("prendas").document(prenda.id)
                                .update(mapOf(
                                    "estadoPublicacion" to "OBSERVADA",
                                    "observacion" to motivo
                                ))
                                .addOnSuccessListener {
                                    Toast.makeText(this@CatalogActivity, "OBSERVACIÓN ENVIADA", Toast.LENGTH_SHORT).show()
                                    loadProducts()
                                    customDialog.dismiss()
                                    dialog.dismiss()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this@CatalogActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        } else {
                            tilMotivo.error = getString(R.string.error_empty_observation)
                        }
                    }
                    customDialog.show()
                }

                optDelete.setOnClickListener {
                    optionsDialog.dismiss()
                    val dialogView = layoutInflater.inflate(R.layout.dialog_admin_delete, null)
                    val customDialog = androidx.appcompat.app.AlertDialog.Builder(this)
                        .setView(dialogView)
                        .create()
                    
                    customDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                    
                    val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
                    val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)

                    btnCancel.setOnClickListener { customDialog.dismiss() }
                    btnConfirm.setOnClickListener {
                        FirebaseFirestore.getInstance().collection("prendas").document(prenda.id)
                            .delete()
                            .addOnSuccessListener {
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
            btnContact.setIconResource(R.drawable.ic_whatsapp)
            btnContact.iconTint = null
            btnAdminMenu.visibility = View.GONE
        }

        // Configurar botón cerrar
        btnCloseDetail.setOnClickListener {
            dialog.dismiss()
        }
        
        // Cargar imagen en el detalle de forma dinámica usando el mismo método robusto que showFullImage
        val imageUriString = prenda.imagenUri
        if (!imageUriString.isNullOrEmpty()) {
            when {
                imageUriString.startsWith("base64:") -> {
                    try {
                        val base64String = imageUriString.substring(7)
                        val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                        val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        ivImage.setImageBitmap(decodedImage)
                    } catch (e: Exception) {
                        ivImage.setImageResource(R.drawable.logo_original)
                    }
                }
                imageUriString.startsWith("http") -> {
                    Glide.with(this).load(imageUriString).into(ivImage)
                }
                imageUriString.startsWith("content://") || imageUriString.startsWith("file://") -> {
                    ivImage.setImageURI(Uri.parse(imageUriString))
                }
                else -> {
                    val imageName = imageUriString.trim().lowercase()
                    val resId = when(imageName) {
                        "chompa_upn" -> R.drawable.chompa_upn
                        "pantalon_upn" -> R.drawable.pantalon_upn
                        "bata_upn" -> R.drawable.bata_upn
                        "casaca_deportiva_upn" -> R.drawable.casaca_deportiva_upn
                        else -> resources.getIdentifier(imageName, "drawable", packageName)
                    }
                    if (resId != 0) ivImage.setImageResource(resId)
                    else ivImage.setImageResource(R.drawable.logo_original)
                }
            }
        } else {
            ivImage.setImageResource(R.drawable.logo_original)
        }

        // --- LÓGICA DEL MAPA DE ENTREGA ---
        val layoutLocation = view.findViewById<View>(R.id.layoutLocation)
        val btnViewLocation = view.findViewById<MaterialButton>(R.id.btnViewLocation)
        val mapView = view.findViewById<com.google.android.gms.maps.MapView>(R.id.mapViewDetail)

        // Si no hay coordenadas, usamos la UPN Breña por defecto para que el botón sea visible
        val lat = prenda.latitud ?: -12.05848
        val lng = prenda.longitud ?: -77.05873
        val ubica = LatLng(lat, lng)

        // El botón ahora siempre será visible para que puedas comprobar su funcionamiento
        btnViewLocation.visibility = View.VISIBLE

        // Gestionar el ciclo de vida del mapa manualmente para evitar que quede en blanco al volver de Maps
        val mapLifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> if (layoutLocation.visibility == View.VISIBLE) mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycle.addObserver(mapLifecycleObserver)
        dialog.setOnDismissListener {
            lifecycle.removeObserver(mapLifecycleObserver)
        }
        
        // Configurar el botón para mostrar el mapa y hacer scroll
        btnViewLocation.setOnClickListener {
            layoutLocation.visibility = View.VISIBLE
            btnViewLocation.visibility = View.GONE 
            
            mapView.onCreate(null)
            mapView.onResume()
            mapView.getMapAsync { googleMap ->
                googleMap.clear()
                googleMap.addMarker(MarkerOptions().position(ubica).title("Punto de entrega"))
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubica, 15f))
                googleMap.uiSettings.isMapToolbarEnabled = false
                googleMap.uiSettings.setAllGesturesEnabled(false)
            }
            
            view.findViewById<NestedScrollView>(R.id.nestedScrollView)?.post {
                view.findViewById<NestedScrollView>(R.id.nestedScrollView)
                    .smoothScrollTo(0, layoutLocation.top)
            }
        }
        
        view.findViewById<View>(R.id.mapOverlay).setOnClickListener {
            val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Punto de Entrega)")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            try {
                startActivity(mapIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Google Maps no está instalado", Toast.LENGTH_SHORT).show()
            }
        }

        // Permitir ver la imagen en grande al hacer clic
        ivImage.setOnClickListener {
            showFullImage(prenda)
        }

        dialog.setContentView(view)

        // Configurar el comportamiento del BottomSheet para que se abra en TODA LA PANTALLA inmediatamente
        dialog.behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isHideable = true
            isFitToContents = true // Se ajusta al contenido (sin espacios blancos)
            peekHeight = resources.displayMetrics.heightPixels
        }

        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT // Cambiado a WRAP_CONTENT para fluidez
        bottomSheet?.requestLayout()

        // Botón de contacto consolidado
        btnContact.setOnClickListener {
            val uid = currentUserId
            if (uid == null) {
                Toast.makeText(this, "Inicie sesión para continuar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // CASO 1: Es el dueño -> Redirigir a edición
            if (isOwner) {
                val intent = Intent(this, DeliverActivity::class.java)
                intent.putExtra("EDIT_PRENDA_ID", prenda.id)
                startActivity(intent)
                dialog.dismiss()
                return@setOnClickListener
            }

            // Prevenir doble clic
            btnContact.isEnabled = false
            val originalText = btnContact.text.toString()
            btnContact.text = "Procesando..."

            val firestore = FirebaseFirestore.getInstance()
            
            // Obtener datos del dueño (teléfono)
            firestore.collection("usuarios").document(prenda.idUsuario)
                .get()
                .addOnSuccessListener { doc ->
                    val telefono = doc.getString("telefono")
                    if (!telefono.isNullOrEmpty()) {
                        if (isAdmin) {
                            // CASO 2: Admin -> Observar y contactar
                            val mensaje = getString(R.string.admin_contact_message, prenda.titulo)
                            firestore.collection("prendas").document(prenda.id)
                                .update(mapOf(
                                    "estadoPublicacion" to "OBSERVADA",
                                    "observacion" to "Publicación bajo supervisión del administrador"
                                ))
                                .addOnSuccessListener { 
                                    loadProducts()
                                    abrirWhatsApp(telefono, mensaje)
                                    dialog.dismiss()
                                }
                                .addOnFailureListener { e ->
                                    btnContact.isEnabled = true
                                    btnContact.text = originalText
                                    Toast.makeText(this, "Error Admin: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            // CASO 3: Estudiante -> Reserva (Chequeo de puntos)
                            firestore.collection("usuarios").document(uid).get()
                                .addOnSuccessListener { userDoc ->
                                    val puntosUsuario = userDoc.getLong("puntos") ?: 0
                                    if (puntosUsuario < prenda.puntos) {
                                        Toast.makeText(this, "Puntos insuficientes (${prenda.puntos} req.)", Toast.LENGTH_SHORT).show()
                                        btnContact.isEnabled = true
                                        btnContact.text = originalText
                                        return@addOnSuccessListener
                                    }

                                    // Transacción de Reserva Robusta
                                    val reserveMsg = getString(R.string.contact_message_template, prenda.titulo)
                                    val userRef = firestore.collection("usuarios").document(uid)
                                    val prendaRef = firestore.collection("prendas").document(prenda.id)

                                    firestore.runTransaction { transaction ->
                                        // LEER DATOS ACTUALES
                                        val userDoc = transaction.get(userRef)
                                        val prendaDoc = transaction.get(prendaRef)
                                        
                                        val puntosActuales = userDoc.getLong("puntos") ?: 0
                                        val estadoActual = prendaDoc.getString("estadoPublicacion") ?: "DISPONIBLE"

                                        if (puntosActuales < prenda.puntos) {
                                            throw Exception("Puntos insuficientes (${prenda.puntos} req.)")
                                        }
                                        
                                        if (estadoActual != "DISPONIBLE") {
                                            throw Exception("Esta prenda ya no está disponible.")
                                        }

                                        // ACTUALIZAR
                                        transaction.update(prendaRef, mapOf(
                                            "estadoPublicacion" to "EN PROCESO",
                                            "receptorId" to uid
                                        ))
                                        transaction.update(userRef, "puntos", puntosActuales - prenda.puntos)
                                        null
                                    }.addOnSuccessListener {
                                        abrirWhatsApp(telefono, reserveMsg)
                                        loadProducts()
                                        dialog.dismiss()
                                    }.addOnFailureListener { e ->
                                        btnContact.isEnabled = true
                                        btnContact.text = originalText
                                        Toast.makeText(this, "Fallo al reservar: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                        }
                    } else {
                        Toast.makeText(this, "El vendedor no tiene teléfono registrado", Toast.LENGTH_SHORT).show()
                        btnContact.isEnabled = true
                        btnContact.text = originalText
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                    btnContact.isEnabled = true
                    btnContact.text = originalText
                }
        }
        
        dialog.show()
    }

    // Función auxiliar para abrir WhatsApp de forma segura
    private fun abrirWhatsApp(telefono: String, mensaje: String) {
        val whatsappUriUri = Uri.parse("whatsapp://send?phone=51$telefono&text=${Uri.encode(mensaje)}")
        val intentNative = Intent(Intent.ACTION_VIEW, whatsappUriUri)
        try {
            startActivity(intentNative)
        } catch (eNative: Exception) {
            val webUrl = "https://api.whatsapp.com/send?phone=51$telefono&text=${Uri.encode(mensaje)}"
            val intentWeb = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
            try {
                startActivity(intentWeb)
            } catch (eWeb: Exception) {
                Toast.makeText(this, "Error al abrir WhatsApp", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Muestra la imagen de la prenda a pantalla completa con soporte de gestos de zoom (Pinch-to-Zoom y arrastre)
    @SuppressLint("DiscouragedApi", "ClickableViewAccessibility")
    private fun showFullImage(prenda: Prenda) {
        val fullImageDialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        fullImageDialog.setContentView(R.layout.dialog_full_image)
        
        val ivFull = fullImageDialog.findViewById<ImageView>(R.id.ivFullImage)
        val btnClose = fullImageDialog.findViewById<ImageButton>(R.id.btnCloseFullImage)
        
        // Cargar imagen
        val imageUriString = prenda.imagenUri
        if (!imageUriString.isNullOrEmpty()) {
            when {
                imageUriString.startsWith("base64:") -> {
                    try {
                        val base64String = imageUriString.substring(7)
                        val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                        val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        ivFull.setImageBitmap(decodedImage)
                    } catch (e: Exception) {
                        ivFull.setImageResource(android.R.drawable.ic_menu_gallery)
                    }
                }
                imageUriString.startsWith("http") -> {
                    Glide.with(this).load(imageUriString).into(ivFull)
                }
                imageUriString.startsWith("content://") || imageUriString.startsWith("file://") -> {
                    ivFull.setImageURI(Uri.parse(imageUriString))
                }
                else -> {
                    val imageName = imageUriString.trim().lowercase()
                    val resId = when(imageName) {
                        "chompa_upn" -> R.drawable.chompa_upn
                        "pantalon_upn" -> R.drawable.pantalon_upn
                        "bata_upn" -> R.drawable.bata_upn
                        "casaca_deportiva_upn" -> R.drawable.casaca_deportiva_upn
                        else -> resources.getIdentifier(imageName, "drawable", packageName)
                    }
                    if (resId != 0) ivFull.setImageResource(resId)
                    else ivFull.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            }
        } else {
            ivFull.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // Implementación nativa de Gesto Pinch-to-Zoom y Arrastre sin librerías externas
        var scaleFactor = 1.0f
        val scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(1.0f, 5.0f) // Límite de zoom entre 1x y 5x
                ivFull.scaleX = scaleFactor
                ivFull.scaleY = scaleFactor
                return true
            }
        })

        var lastTouchX = 0f
        var lastTouchY = 0f
        var posX = 0f
        var posY = 0f

        ivFull.setOnTouchListener { v, event ->
            scaleGestureDetector.onTouchEvent(event)
            
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.rawX
                    lastTouchY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    if (scaleFactor > 1.0f) { // Solo permitir arrastrar si hay zoom aplicado
                        val deltaX = event.rawX - lastTouchX
                        val deltaY = event.rawY - lastTouchY
                        
                        posX += deltaX
                        posY += deltaY
                        
                        v.translationX = posX
                        v.translationY = posY
                        
                        lastTouchX = event.rawX
                        lastTouchY = event.rawY
                    }
                }
                MotionEvent.ACTION_UP -> {
                    // Si se remueve el zoom, resetear la posición centrada suavemente
                    if (scaleFactor <= 1.0f) {
                        posX = 0f
                        posY = 0f
                        v.animate().translationX(0f).translationY(0f).setDuration(200).start()
                    }
                }
            }
            true
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
            chip.chipStrokeWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics
            )
            chip.setTextColor(ContextCompat.getColorStateList(this, R.color.chip_text_selector))
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
            chip.chipStrokeWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics
            )
            chip.setTextColor(ContextCompat.getColorStateList(this, R.color.chip_text_selector))
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
            chip.chipStrokeWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics
            )
            chip.setTextColor(ContextCompat.getColorStateList(this, R.color.chip_text_selector))
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
    // Cargamos los productos desde Firestore
    private fun loadProducts() {
        FirebaseFirestore.getInstance().collection("prendas")
            .orderBy("fechaCreacion", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val products = mutableListOf<Prenda>()
                for (document in result) {
                    val prenda = Prenda(
                        id = document.id,
                        idUsuario = document.getString("vendedorId") ?: "",
                        titulo = document.getString("titulo") ?: "",
                        descripcion = document.getString("descripcion") ?: "",
                        carrera = document.getString("carrera") ?: "",
                        talla = document.getString("talla") ?: "",
                        genero = document.getString("genero") ?: "",
                        tipoTransaccion = document.getString("tipo") ?: "",
                        puntos = document.getLong("puntos")?.toInt() ?: 0,
                        imagenUri = document.getString("urlImagen"),
                        estado = document.getString("estadoPublicacion") ?: "DISPONIBLE",
                        idReceptor = document.getString("receptorId"),
                        estadoFisico = document.getString("estadoFisico") ?: "Usado",
                        observacion = document.getString("observacion"),
                        latitud = document.getDouble("latitud"),
                        longitud = document.getDouble("longitud")
                    )

                    val isAdmin = currentRol == "ADMIN"
                    val isOwner = prenda.idUsuario == currentUserId
                    
                    // Mostramos la prenda si:
                    // 1. Está disponible (para todos)
                    // 2. El usuario es ADMIN (ve todo excepto quizá lo borrado)
                    // 3. El usuario es el DUEÑO (ve sus publicaciones en cualquier estado)
                    if (prenda.estado == "DISPONIBLE" || isAdmin || isOwner) {
                        products.add(prenda)
                    }
                }
                
                allProducts = products
                applyFilters()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar catálogo: ${it.message}", Toast.LENGTH_SHORT).show()
            }
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
            
            val isAdmin = currentRol == "ADMIN"
            val isOwner = prenda.idUsuario == currentUserId
            
            // Lógica de visibilidad refinada
            val isVisible = when (prenda.estado) {
                "DISPONIBLE" -> true
                "OBSERVADA" -> isAdmin || isOwner
                "EN PROCESO" -> isAdmin || isOwner // Solo el admin o el dueño ven lo reservado en el catálogo general
                else -> isAdmin // Otros estados como 'ENTREGADO' solo para admin
            }
            
            matchesSearch && matchesCarrera && matchesTalla && matchesGenero && isVisible
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
