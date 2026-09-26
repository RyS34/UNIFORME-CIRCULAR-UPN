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

class ExchangesActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper
    private lateinit var adapter: MyExchangesAdapter
    private var currentUserId: Int = -1
    private var currentTab: Int = 0 // 0: Aportes, 1: Adquisiciones

    // Configuración de la actividad de intercambios
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exchanges)
        // Inicializar la base de datos
        dbHelper = DBHelper(this)

        // Obtener el ID del usuario actual mediante el nombre enviado desde el MainActivity
        val username = intent.getStringExtra("USER_NAME") ?: "Usuario"
        currentUserId = dbHelper.obtenerUsuarioId(username)

        // Ajustar insets para diseño Edge-to-Edge inmersivo usando el espaciador dinámico
        val rootView = findViewById<View>(R.id.mainExchanges)
        val statusBarSpacer = findViewById<View>(R.id.statusBarSpacer)
        val recyclerView = findViewById<RecyclerView>(R.id.rvExchanges)
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
            onConfirmClick = { prenda ->
                confirmExchange(prenda)
            },
            onCancelClick = { prenda ->
                cancelReservation(prenda)
            }
        )
        recyclerView.adapter = adapter
    }

    // Confirmamos el intercambio de una prenda
    private fun confirmExchange(prenda: Prenda) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Entrega")
            .setMessage("¿Confirmas que ya entregaste la prenda '${prenda.titulo}'? Esto cerrará el ciclo y otorgará los puntos.")
            .setPositiveButton("Sí, entregada") { _, _ ->
                if (dbHelper.marcarComoCanjeada(prenda.id)) {
                    // El dueño de la prenda recibe los puntos correspondientes
                    dbHelper.sumarPuntosUsuario(prenda.idUsuario, prenda.puntos)
                    Toast.makeText(this, "¡Canje completado exitosamente! Has ganado ${prenda.puntos} puntos.", Toast.LENGTH_SHORT).show()
                    loadData()
                }
            }
            .setNegativeButton("Aún no", null)
            .show()
    }
    // Cancelamos la reserva de una prenda y devolvemos los puntos al dueño
    private fun cancelReservation(prenda: Prenda) {
        AlertDialog.Builder(this)
            .setTitle("Cancelar Reserva")
            .setMessage("¿Deseas cancelar el interés de este estudiante? La prenda volverá a estar disponible para todos en el catálogo.")
            .setPositiveButton("Sí, liberar") { _, _ ->
                if (dbHelper.cancelarReserva(prenda.id)) {
                    // Devolver puntos al receptor si existía uno
                    prenda.idReceptor?.let { receptorId ->
                        dbHelper.sumarPuntosUsuario(receptorId, prenda.puntos)
                    }
                    Toast.makeText(this, "Prenda liberada y puntos devueltos al interesado", Toast.LENGTH_SHORT).show()
                    loadData()
                }
            }
            .setNegativeButton("Mantener reserva", null)
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
            if (receptorId != null && receptorId != 0) {
                val receptorName = dbHelper.obtenerNombreUsuario(receptorId)
                tvUser.text = receptorName ?: "Interesado ID: $receptorId"
            } else {
                tvUser.text = "Sin reservar"
            }
        } else {
            tvMainTitle.text = "Detalle de mi Canje"
            val ownerName = dbHelper.obtenerNombreUsuario(prenda.idUsuario)
            tvUser.text = ownerName ?: "Dueño ID: ${prenda.idUsuario}"
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
                        putExtra("USER_NAME", dbHelper.obtenerNombreUsuario(currentUserId))
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

        // Cargar imagen
        val imageUriString = prenda.imagenUri
        if (imageUriString != null && (imageUriString.startsWith("content://") || imageUriString.startsWith("file://"))) {
            ivImage.setImageURI(Uri.parse(imageUriString))
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
            if (resId != 0) ivImage.setImageResource(resId)
            else ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.setContentView(view)
        
        // Configurar el comportamiento del BottomSheet para que se expanda por completo al abrirse
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true // Evita que se quede a la mitad al arrastrar hacia abajo
        }

        dialog.show()
    }
    // Confirmamos la eliminación de una prenda del catálogo
    private fun confirmDelete(prenda: Prenda) {
        if (currentTab == 1) return

        AlertDialog.Builder(this)
            .setTitle("Eliminar Aporte")
            .setMessage("¿Estás seguro de que deseas eliminar esta prenda? Se quitará del catálogo público.")
            .setPositiveButton("Eliminar") { _, _ ->
                // Si la prenda estaba reservada (EN PROCESO), devolvemos los puntos al receptor
                if (prenda.estado == "EN PROCESO") {
                    prenda.idReceptor?.let { receptorId ->
                        dbHelper.sumarPuntosUsuario(receptorId, prenda.puntos)
                    }
                }

                if (dbHelper.eliminarPrenda(prenda.id)) {
                    val msg = if (prenda.estado == "EN PROCESO") 
                        "Prenda eliminada y puntos devueltos al interesado" 
                    else "Prenda eliminada correctamente"
                    
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    loadData()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    // Cargamos los datos desde la base de datos y los mostramos en el RecyclerView
    private fun loadData() {
        val cursor = dbHelper.obtenerCatalogo()
        val products = mutableListOf<Prenda>()

        if (cursor.moveToFirst()) {
            do {
                val userIdPrenda = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_ID_USUARIO))
                val receptorId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_ID_RECEPTOR))
                
                val shouldAdd = if (currentTab == 0) {
                    userIdPrenda == currentUserId // Mis Aportes
                } else {
                    receptorId == currentUserId // Mis Adquisiciones
                }

                if (shouldAdd) {
                    products.add(
                        Prenda(
                            cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_ID)),
                            userIdPrenda,
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
                            else receptorId,
                            cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_ESTADO_FISICO)),
                            cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_OBSERVACION))
                        )
                    )
                }
            } while (cursor.moveToNext())
        }
        cursor.close()

        // Actualizar el adaptador con los nuevos datos y ejecutar la animación
        adapter.updateList(products)
        
        // Ejecutar animación de la lista cada vez que se filtran/cambian datos
        val recyclerView = findViewById<RecyclerView>(R.id.rvExchanges)
        recyclerView.scheduleLayoutAnimation()

        // Actualizar UI según la pestaña
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
        // Mostrar/ocultar layout de datos vacíos según la lista
        val layoutEmptyState = findViewById<LinearLayout>(R.id.layoutEmptyState)
        layoutEmptyState.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun corregirPrendaDirectamente(idPrenda: Int) {
        if (dbHelper.corregirPrenda(idPrenda)) {
            Toast.makeText(this, "Publicación corregida y enviada para revisión", Toast.LENGTH_SHORT).show()
            loadData() // Recargar la lista inmediatamente
        } else {
            Toast.makeText(this, "Error al guardar los cambios", Toast.LENGTH_SHORT).show()
        }
    }
}

