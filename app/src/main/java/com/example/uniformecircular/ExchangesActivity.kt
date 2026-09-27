package com.example.uniformecircular

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AlertDialog
import android.widget.ImageView
import android.widget.Toast
import com.google.android.material.tabs.TabLayout

import android.view.animation.AnimationUtils
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.bumptech.glide.Glide
import androidx.core.net.toUri

class ExchangesActivity : AppCompatActivity() {

    private lateinit var adapter: MyExchangesAdapter
    private var currentUserId: String? = null
    private var currentTab: Int = 0 // 0: Aportes, 1: Adquisiciones

    // Configuración de la actividad de intercambios
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exchanges)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        // Ajustar insets para diseño Edge-to-Edge inmersivo usando el espaciador dinámico
        val rootView = findViewById<View>(R.id.mainExchanges)
        val statusBarSpacer = findViewById<View>(R.id.statusBarSpacer)
        val layoutContent = findViewById<View>(R.id.layoutContent)

        // Configurar el espaciador dinámico para el status bar
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
        // Configurar el RecyclerView
        setupRecyclerView()
        setupTabs()

        // Manejar el intent para seleccionar una pestaña específica (0: Aportes, 1: Adquisiciones)
        val tabToSelect = intent.getIntExtra("SELECT_TAB", -1)
        if (tabToSelect != -1) {
            val tabLayout = findViewById<TabLayout>(R.id.tabLayoutExchanges)
            tabLayout.getTabAt(tabToSelect)?.select()
            currentTab = tabToSelect
        }
    }
    // Configuramos las pestañas de intercambios
    private fun setupTabs() {
        val tabLayout = findViewById<TabLayout>(R.id.tabLayoutExchanges)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                loadData()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    // Cargamos los datos desde la base de datos cada vez que la pantalla vuelve a estar en primer plano
    override fun onResume() {
        super.onResume()
        loadData()
    }

    //Se configura el RecyclerView
    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.rvExchanges)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // Cargar animación de entrada en cascada
        val animation = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_slide_left)
        recyclerView.layoutAnimation = animation

        adapter = MyExchangesAdapter(emptyList(), currentUserId,
            onItemClick = { prenda ->
                showPrendaDetail(prenda)
            },
            onDeleteClick = { prenda ->
                confirmDelete(prenda)
            },
            onConfirmClick = { _ ->
                confirmExchange()
            },
            onCancelClick = { prenda ->
                cancelReservation(prenda)
            },
            onShowQRClick = { prenda ->
                showExchangeQR(prenda)
            }
        )
        recyclerView.adapter = adapter
    }

    // Confirmamos el intercambio de una prenda mediante el escaneo de un código QR seguro
    private fun confirmExchange() {
        val options = com.journeyapps.barcodescanner.ScanOptions().apply {
            setDesiredBarcodeFormats(com.journeyapps.barcodescanner.ScanOptions.QR_CODE)
            setPrompt(getString(R.string.qr_scan_prompt))
            setCameraId(0) // Usar cámara trasera
            setBeepEnabled(true)
            setBarcodeImageEnabled(false)
            setOrientationLocked(true)
        }
        qrScannerLauncher.launch(options)
    }

    // Registrador del resultado del escaneo QR
    private val qrScannerLauncher = registerForActivityResult(com.journeyapps.barcodescanner.ScanContract()) { result ->
        if (result.contents != null) {
            val datos = result.contents.split("|")
            if (datos.size >= 2) {
                val prendaIdEscaneado = datos[0]
                val receptorIdEscaneado = datos[1]

                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("prendas").document(prendaIdEscaneado).get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            val vendedorId = doc.getString("vendedorId") ?: ""
                            val receptorIdActual = doc.getString("receptorId") ?: ""
                            val estadoPublicacion = doc.getString("estadoPublicacion") ?: ""
                            val puntosPrenda = doc.getLong("puntos")?.toInt() ?: 0

                            if (vendedorId != currentUserId) {
                                Toast.makeText(this, getString(R.string.qr_error_not_owner), Toast.LENGTH_LONG).show()
                                return@addOnSuccessListener
                            }

                            if (receptorIdActual != receptorIdEscaneado) {
                                Toast.makeText(this, getString(R.string.qr_error_wrong_user), Toast.LENGTH_LONG).show()
                                return@addOnSuccessListener
                            }

                            if (estadoPublicacion == "CANJEADO") {
                                Toast.makeText(this, getString(R.string.qr_error_already_exchanged), Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }

                            // Si todo es correcto, realizamos la entrega de manera atómica
                            firestore.runTransaction { transaction ->
                                val userRef = firestore.collection("usuarios").document(vendedorId)
                                val userDoc = transaction.get(userRef)
                                val puntosActuales = userDoc.getLong("puntos") ?: 0

                                transaction.update(doc.reference, "estadoPublicacion", "CANJEADO")
                                transaction.update(userRef, "puntos", puntosActuales + puntosPrenda)
                                null
                            }.addOnSuccessListener {
                                AlertDialog.Builder(this)
                                    .setTitle(getString(R.string.qr_success_title))
                                    .setMessage(getString(R.string.qr_success_delivery, puntosPrenda))
                                    .setPositiveButton(getString(R.string.btn_understand)) { dialog, _ ->
                                        dialog.dismiss()
                                        loadData()
                                    }
                                    .setCancelable(false)
                                    .show()
                            }.addOnFailureListener { e ->
                                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, getString(R.string.qr_error_not_exists), Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, getString(R.string.qr_error_invalid), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, getString(R.string.qr_scan_cancelled), Toast.LENGTH_SHORT).show()
        }
    }

    // Genera y muestra un código QR único para que el comprador valide su canje
    private fun showExchangeQR(prenda: Prenda) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_show_qr, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val ivQrCode = dialogView.findViewById<ImageView>(R.id.ivQrCodeGenerated)
        val tvQrTitle = dialogView.findViewById<TextView>(R.id.tvQrTitle)
        val tvQrDesc = dialogView.findViewById<TextView>(R.id.tvQrDescription)
        val btnCloseQr = dialogView.findViewById<Button>(R.id.btnCloseQrDialog)

        tvQrTitle.text = prenda.titulo
        tvQrDesc.text = getString(R.string.qr_dialog_desc)

        // Contenido del QR: ID de Prenda y ID de Receptor separados por un pipe |
        val qrContent = "${prenda.id}|${currentUserId}"

        try {
            val barcodeEncoder = com.journeyapps.barcodescanner.BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(qrContent, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512)
            ivQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.qr_gen_error, e.message), Toast.LENGTH_SHORT).show()
        }

        btnCloseQr.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
    // Cancelamos la reserva de una prenda y devolvemos los puntos al receptor
    private fun cancelReservation(prenda: Prenda) {
        val isOwner = prenda.idUsuario == currentUserId
        val title = if (isOwner) getString(R.string.cancel_title_owner) else getString(R.string.cancel_title_receptor)
        val message = if (isOwner) 
            getString(R.string.cancel_msg_owner)
            else getString(R.string.cancel_msg_receptor, prenda.puntos)

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.cancel_btn_positive)) { _, _ ->
                val firestore = FirebaseFirestore.getInstance()
                
                // Usamos una transacción para asegurar que la devolución de puntos sea atómica
                val prendaRef = firestore.collection("prendas").document(prenda.id)
                val receptorId = prenda.idReceptor

                if (receptorId != null) {
                    val userRef = firestore.collection("usuarios").document(receptorId)
                    
                    firestore.runTransaction { transaction ->
                        // LEER AMBOS DOCUMENTOS PRIMERO (Obligatorio en transacciones)
                        val userDoc = transaction.get(userRef)
                        transaction.get(prendaRef)
                        
                        val puntosActuales = userDoc.getLong("puntos") ?: 0
                        
                        // ACTUALIZAR
                        transaction.update(prendaRef, mapOf(
                            "estadoPublicacion" to "DISPONIBLE",
                            "receptorId" to null
                        ))
                        transaction.update(userRef, "puntos", puntosActuales + prenda.puntos)
                        null
                    }.addOnSuccessListener {
                        Toast.makeText(this, getString(R.string.cancel_success), Toast.LENGTH_SHORT).show()
                        loadData()
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Fallback si no hay receptor por alguna razón
                    prendaRef.update(mapOf(
                        "estadoPublicacion" to "DISPONIBLE",
                        "receptorId" to null
                    )).addOnSuccessListener {
                        loadData()
                    }
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }
    //Se muestra el detalle de la prenda en un BottomSheetDialog
    private fun showPrendaDetail(prenda: Prenda) {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.dialog_exchange_detail, null, false)

        val ivImage = view.findViewById<ImageView>(R.id.ivExchangeDetailImage)
        val tvTitle = view.findViewById<TextView>(R.id.tvExchangeDetailName)
        val tvStatus = view.findViewById<TextView>(R.id.tvExchangeDetailStatus)
        val tvPoints = view.findViewById<TextView>(R.id.tvExchangeDetailPoints)
        val tvCategory = view.findViewById<TextView>(R.id.tvExchangeDetailCategory)
        val tvType = view.findViewById<TextView>(R.id.tvExchangeDetailType)
        val tvUser = view.findViewById<TextView>(R.id.tvExchangeDetailUser)
        val tvDescription = view.findViewById<TextView>(R.id.tvExchangeDetailDescription)
        val btnClose = view.findViewById<Button>(R.id.btnExchangeDetailClose)
        val btnEdit = view.findViewById<View>(R.id.btnEditPrenda)
        val tvMainTitle = view.findViewById<TextView>(R.id.tvExchangeDetailTitle)

        // Configurar título y usuario relacionado
        if (currentTab == 0) {
            tvMainTitle.text = "Detalle de mi Aporte"
            val receptorId = prenda.idReceptor
            if (!receptorId.isNullOrEmpty()) {
                FirebaseFirestore.getInstance().collection("usuarios").document(receptorId).get()
                    .addOnSuccessListener { doc ->
                        tvUser.text = doc.getString("usuario") ?: "Estudiante"
                    }
            } else {
                tvUser.text = "Sin reservar"
            }
        } else {
            tvMainTitle.text = "Detalle de mi Canje"
            FirebaseFirestore.getInstance().collection("usuarios").document(prenda.idUsuario).get()
                .addOnSuccessListener { doc ->
                    tvUser.text = doc.getString("usuario") ?: "Estudiante"
                }
        }
        // Configurar detalles de la prenda en el diálogo
        tvTitle.text = prenda.titulo
        tvPoints.text = getString(R.string.label_points, prenda.puntos)
        tvCategory.text = getString(R.string.label_category_talla_genero, prenda.carrera, prenda.talla, prenda.genero)
        tvType.text = prenda.tipoTransaccion
        tvDescription.text = prenda.descripcion

        // Configurar Panel de Observación si aplica
        val layoutObs = view.findViewById<View>(R.id.layoutObservation)
        val tvObsText = view.findViewById<TextView>(R.id.tvObservationText)

        if (prenda.estado == "OBSERVADA") {
            layoutObs.visibility = View.VISIBLE
            tvObsText.text = prenda.observacion ?: "Sin motivo especificado"
            tvStatus.text = "POR CORREGIR"
            
            // Si el usuario es el dueño, mostrar botón de corregir
            if (prenda.idUsuario == currentUserId) {
                btnEdit.visibility = View.VISIBLE
                btnEdit.setOnClickListener {
                    dialog.dismiss()
                    val intent = Intent(this@ExchangesActivity, DeliverActivity::class.java).apply {
                        putExtra("EDIT_PRENDA_ID", prenda.id)
                    }
                    startActivity(intent)
                }
            }
        } else {
            layoutObs.visibility = View.GONE
            tvStatus.text = prenda.estado
            btnEdit.visibility = View.GONE
        }

        // Configurar estado de la prenda en el diálogo
        when (prenda.estado) {
            "CANJEADO" -> {
                tvStatus.setTextColor(getColor(R.color.slate_500))
                tvStatus.setBackgroundResource(R.drawable.bg_badge_exchanged)
            }
            "EN PROCESO", "OBSERVADA" -> {
                tvStatus.setTextColor(getColor(R.color.amber_600))
                tvStatus.setBackgroundResource(R.drawable.bg_badge_in_progress)
            }
            else -> {
                tvStatus.setTextColor(getColor(R.color.emerald_500))
                tvStatus.setBackgroundResource(R.drawable.bg_badge_available)
            }
        }

        // CONTROL DE ACCESO PREMIUM: Solo el dueño ve el botón de cerrar si no le corresponde actuar,
        // o podríamos ocultar acciones si no es el dueño. 
        // En este diálogo solo hay un botón de "Cerrar", las acciones están en la tarjeta.

        // Cargar imagen de forma dinámica
        val imageUriString = prenda.imagenUri
        if (!imageUriString.isNullOrEmpty()) {
            when {
                imageUriString.startsWith("base64:") -> {
                    try {
                        val base64String = imageUriString.substring(7)
                        val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                        val decodedImage = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        ivImage.setImageBitmap(decodedImage)
                    } catch (e: Exception) {
                        ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
                    }
                }
                imageUriString.startsWith("http") -> {
                    Glide.with(this).load(imageUriString).into(ivImage)
                }
                imageUriString.startsWith("content://") || imageUriString.startsWith("file://") -> {
                    ivImage.setImageURI(imageUriString.toUri())
                }
                else -> {
                    val resId = when(val imageName = imageUriString.trim().lowercase()) {
                        "chompa_upn" -> R.drawable.chompa_upn
                        "pantalon_upn" -> R.drawable.pantalon_upn
                        "bata_upn" -> R.drawable.bata_upn
                        "casaca_deportiva_upn" -> R.drawable.casaca_deportiva_upn
                        else -> resources.getIdentifier(imageName, "drawable", packageName)
                    }
                    if (resId != 0) ivImage.setImageResource(resId)
                    else ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            }
        } else {
            ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.setContentView(view)
        
        // Configurar el comportamiento del BottomSheet para que se expanda por completo al abrirse
        view.post {
            val parent = view.parent as? View
            parent?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        dialog.show()
    }
    // Confirmamos la eliminación de una prenda del catálogo
    private fun confirmDelete(prenda: Prenda) {
        if (currentTab == 1) return

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_title))
            .setMessage(getString(R.string.delete_msg))
            .setPositiveButton(getString(R.string.delete_btn_positive)) { _, _ ->
                val firestore = FirebaseFirestore.getInstance()
                
                val prendaRef = firestore.collection("prendas").document(prenda.id)
                val receptorId = prenda.idReceptor

                if (prenda.estado == "EN PROCESO" && receptorId != null) {
                    val userRef = firestore.collection("usuarios").document(receptorId)
                    
                    firestore.runTransaction { transaction ->
                        val userDoc = transaction.get(userRef)
                        val points = userDoc.getLong("puntos") ?: 0
                        
                        transaction.delete(prendaRef)
                        transaction.update(userRef, "puntos", points + prenda.puntos)
                        null
                    }.addOnSuccessListener {
                        Toast.makeText(this, getString(R.string.delete_success_refund), Toast.LENGTH_SHORT).show()
                        loadData()
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, "Error transaction: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    prendaRef.delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, getString(R.string.delete_success), Toast.LENGTH_SHORT).show()
                            loadData()
                        }.addOnFailureListener { e ->
                            Toast.makeText(this, "Error delete: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }
    // Cargamos los datos desde Firestore y los mostramos en el RecyclerView
    private fun loadData() {
        currentUserId?.let { uid ->
            val query = if (currentTab == 0) {
                FirebaseFirestore.getInstance().collection("prendas")
                    .whereEqualTo("vendedorId", uid)
            } else {
                FirebaseFirestore.getInstance().collection("prendas")
                    .whereEqualTo("receptorId", uid)
            }

            query.get().addOnSuccessListener { result ->
                val products = result.map { doc ->
                    Prenda(
                        id = doc.id,
                        idUsuario = doc.getString("vendedorId") ?: "",
                        titulo = doc.getString("titulo") ?: "",
                        descripcion = doc.getString("descripcion") ?: "",
                        carrera = doc.getString("carrera") ?: "",
                        talla = doc.getString("talla") ?: "",
                        genero = doc.getString("genero") ?: "",
                        tipoTransaccion = doc.getString("tipo") ?: "",
                        puntos = doc.getLong("puntos")?.toInt() ?: 0,
                        imagenUri = doc.getString("urlImagen"),
                        estado = doc.getString("estadoPublicacion") ?: "DISPONIBLE",
                        idReceptor = doc.getString("receptorId"),
                        estadoFisico = doc.getString("estadoFisico") ?: "Usado",
                        observacion = doc.getString("observacion")
                    )
                }

                adapter.updateList(products)
                findViewById<RecyclerView>(R.id.rvExchanges).scheduleLayoutAnimation()

                // Actualizar UI
                val tvTotal = findViewById<TextView>(R.id.tvTotalContributions)
                val layoutSummary = findViewById<LinearLayout>(R.id.layoutSummary)
                val tvEmptyTitle = findViewById<TextView>(R.id.tvEmptyTitle)
                val tvEmptyDesc = findViewById<TextView>(R.id.tvEmptyDesc)
                
                if (currentTab == 0) {
                    layoutSummary.visibility = View.VISIBLE
                    tvTotal.text = if (products.size == 1) getString(R.string.label_contribution_single) 
                                   else getString(R.string.label_contribution_count, products.size)
                    tvEmptyTitle.setText(R.string.exchanges_empty_title)
                    tvEmptyDesc.setText(R.string.exchanges_empty_desc)
                } else {
                    layoutSummary.visibility = View.GONE
                    tvEmptyTitle.setText(R.string.exchanges_empty_acquisitions_title)
                    tvEmptyDesc.setText(R.string.exchanges_empty_acquisitions_desc)
                }
                
                findViewById<LinearLayout>(R.id.layoutEmptyState).visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

}


