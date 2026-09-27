package com.example.uniformecircular

import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: ProductAdapter
    private var allProducts: List<Prenda> = listOf()

    private var currentUserId: String? = null
    private var currentUserName: String = "Usuario"

    private fun Int.toPx(context: android.content.Context): Int = (this * context.resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val user = FirebaseAuth.getInstance().currentUser
        currentUserId = user?.uid

        // Ajustar insets para que el fondo sea total
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            findViewById<View>(R.id.linearLayoutTop)?.setPadding(24.toPx(this), systemBars.top + 10.toPx(this), 24.toPx(this), 10.toPx(this))
            insets
        }
        // Configurar vistas y datos iniciales del usuario
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val etSearch = findViewById<EditText>(R.id.etMainSearch)
        val imgAvatarMain = findViewById<View>(R.id.imgAvatarMain)

        currentUserId?.let { uid ->
            FirebaseFirestore.getInstance().collection("usuarios").document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    currentUserName = doc.getString("nombre") ?: doc.getString("usuario") ?: "Usuario"
                    tvWelcome.text = currentUserName
                    actualizarPuntosVista()
                }
        }

        setupRecyclerView()
        setupSearch(etSearch)

        // Perfil Listener
        imgAvatarMain?.setOnClickListener {
            val intent = Intent(this, AccountActivity::class.java).apply {
                putExtra("USER_NAME", currentUserName)
            }
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_right, R.anim.slide_out_left)
            startActivity(intent, options.toBundle())
        }

        // Menu Listeners
        findViewById<View>(R.id.menuCatalog)?.setOnClickListener {
            val intent = Intent(this, CatalogActivity::class.java).apply {
                putExtra("USER_NAME", currentUserName)
            }
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_right, R.anim.slide_out_left)
            startActivity(intent, options.toBundle())
        }
        findViewById<View>(R.id.menuDeliver)?.setOnClickListener {
            val intent = Intent(this, DeliverActivity::class.java).apply {
                putExtra("USER_NAME", currentUserName)
            }
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_right, R.anim.slide_out_left)
            startActivity(intent, options.toBundle())
        }
        findViewById<View>(R.id.menuExchanges)?.setOnClickListener {
            val intent = Intent(this, ExchangesActivity::class.java).apply {
                putExtra("USER_NAME", currentUserName)
            }
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_right, R.anim.slide_out_left)
            startActivity(intent, options.toBundle())
        }
        findViewById<View>(R.id.menuHelp)?.setOnClickListener {
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_right, R.anim.slide_out_left)
            startActivity(Intent(this, HelpActivity::class.java), options.toBundle())
        }

        // Clic en la tarjeta de Impacto para ver un resumen rápido
        findViewById<View>(R.id.cardImpact)?.setOnClickListener {
            mostrarResumenImpacto(currentUserName)
        }

        // Categorías Populares
        findViewById<View>(R.id.btnCatHealth)?.setOnClickListener { openCatalogWithFilter("Salud") }
        findViewById<View>(R.id.btnCatEng)?.setOnClickListener { openCatalogWithFilter("Ingeniería") }
        findViewById<View>(R.id.btnCatBiz)?.setOnClickListener { openCatalogWithFilter("Negocios") }
        findViewById<View>(R.id.btnCatAll)?.setOnClickListener { openCatalogWithFilter("Todas") }

        // Ver Todos (Recién llegados)
        findViewById<View>(R.id.tvRecentViewAll)?.setOnClickListener { openCatalogWithFilter("Todas") }
    }
    @SuppressLint("InflateParams")
    private fun mostrarResumenImpacto(currentUserName: String) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.dialog_impact_summary, null)

        val rvContributions = view.findViewById<RecyclerView>(R.id.rvImpactContributions)
        val btnManage = view.findViewById<Button>(R.id.btnViewFullExchanges)
        val tvDesc = view.findViewById<TextView>(R.id.tvImpactDialogDesc)
        val tvStatAportes = view.findViewById<TextView>(R.id.tvStatAportes)
        val tvStatPuntos = view.findViewById<TextView>(R.id.tvStatPuntos)

        // Obtener prendas del usuario desde Firestore
        currentUserId?.let { uid ->
            FirebaseFirestore.getInstance().collection("prendas")
                .whereEqualTo("vendedorId", uid)
                .get()
                .addOnSuccessListener { result ->
                    val misPrendas = result.map { doc ->
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
                    
                    val puntosGenerados = misPrendas.sumOf { it.puntos }
                    tvStatAportes.text = misPrendas.size.toString()
                    tvStatPuntos.text = puntosGenerados.toString()

                    if (misPrendas.isEmpty()) {
                        tvDesc.text = "Aún no has realizado aportes. ¡Empieza hoy!"
                        view.findViewById<View>(R.id.layoutStats).visibility = View.GONE
                    } else {
                        tvDesc.text = "Has contribuido a la economía circular de la UPN"
                    }

                    rvContributions.layoutManager = LinearLayoutManager(this)
                    val impactAdapter = ProductAdapter(misPrendas, isCompact = true) { prenda ->
                        if (prenda.estado == "OBSERVADA") {
                            val intent = Intent(this, ExchangesActivity::class.java).apply {
                                putExtra("USER_NAME", currentUserName)
                                putExtra("SELECT_TAB", 0)
                            }
                            startActivity(intent)
                        } else {
                            val intent = Intent(this, CatalogActivity::class.java).apply {
                                putExtra("PRENDA_ID", prenda.id)
                                putExtra("USER_NAME", currentUserName)
                            }
                            startActivity(intent)
                        }
                        dialog.dismiss()
                    }
                    rvContributions.adapter = impactAdapter
                }
        }

        btnManage.setOnClickListener {
            val intent = Intent(this, ExchangesActivity::class.java).apply {
                putExtra("USER_NAME", currentUserName)
                putExtra("SELECT_TAB", 0)
            }
            startActivity(intent)
            dialog.dismiss()
        }

        dialog.setContentView(view)

        // Configurar el comportamiento del BottomSheet para que se expanda por completo al abrirse
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(it)
            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }

        dialog.show()
    }
    // Configuramos el RecyclerView para mostrar las prendas recientes
    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.rvRecentProducts)
        val currentUserName = intent.getStringExtra("USER_NAME") ?: "Usuario"
        adapter = ProductAdapter(emptyList(), isCompact = false) { prenda ->
            // Abrir catálogo con el producto seleccionado (o mostrar detalle directo)
            val intent = Intent(this, CatalogActivity::class.java).apply {
                putExtra("PRENDA_ID", prenda.id)
                putExtra("USER_NAME", currentUserName)
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter
    }
    // Configuramos el buscador para filtrar las prendas según el texto ingresado
    private fun setupSearch(etSearch: EditText) {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterProducts(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Si el usuario presiona "Buscar" en el teclado, llevarlo al catálogo con el texto
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val intent = Intent(this, CatalogActivity::class.java).apply {
                    putExtra("SEARCH_QUERY", etSearch.text.toString())
                    putExtra("USER_NAME", currentUserName)
                }
                startActivity(intent)
                true
            } else false
        }
    }
    // Abrimos el catálogo con el filtro seleccionado (o sin filtro)
    private fun openCatalogWithFilter(category: String) {
        val intent = Intent(this, CatalogActivity::class.java).apply {
            putExtra("SELECTED_CATEGORY", category)
            putExtra("USER_NAME", currentUserName)
        }
        val options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_right, R.anim.slide_out_left)
        startActivity(intent, options.toBundle())
    }
    // Eliminamos las tildes de una cadena de texto para una comparación sin acentos
    private fun String.removeAccents(): String {
        return java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase()
    }
    //Filtrar productos en el catálogo según el texto ingresado
    private fun filterProducts(query: String) {
        val normalizedQuery = query.removeAccents()
        val filtered = allProducts.filter { it.titulo.removeAccents().contains(normalizedQuery) }
        adapter.updateList(filtered)
        
        val tvEmpty = findViewById<TextView>(R.id.tvEmptySearch)
        tvEmpty.visibility = if (filtered.isEmpty() && query.isNotEmpty()) View.VISIBLE else View.GONE
    }
    // Actualizamos los puntos del usuario en la vista de inicio
    override fun onResume() {
        super.onResume()
        // Limpiar el buscador para una nueva experiencia al regresar
        findViewById<EditText>(R.id.etMainSearch)?.setText("")
        loadRecentProducts()
        actualizarPuntosVista()
    }
    // Actualizamos los puntos del usuario en la vista de inicio y el impacto circular (conteo de aportes)
    private fun actualizarPuntosVista() {
        currentUserId?.let { uid ->
            val db = FirebaseFirestore.getInstance()
            db.collection("usuarios").document(uid).get()
                .addOnSuccessListener { doc ->
                    val puntos = doc.getLong("puntos") ?: 0
                    findViewById<TextView>(R.id.tvPointsCount)?.text = "$puntos Pts"
                }

            // Actualizar impacto circular (conteo de aportes exitosos)
            db.collection("prendas")
                .whereEqualTo("vendedorId", uid)
                .get()
                .addOnSuccessListener { result ->
                    val conteoAportes = result.size()
                    val tvImpactDesc = findViewById<TextView>(R.id.tvImpactDesc)
                    tvImpactDesc.text = if (conteoAportes == 1) "1 Uniforme reciclado" else "$conteoAportes Uniformes reciclados"
                }
        }
    }
    // Cargamos las prendas recientes del catálogo y las mostramos en el RecyclerView
    private fun loadRecentProducts() {
        FirebaseFirestore.getInstance().collection("prendas")
            .orderBy("fechaCreacion", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { result ->
                val products = result.mapNotNull { doc ->
                    val estado = doc.getString("estadoPublicacion") ?: "DISPONIBLE"
                    if (estado != "CANJEADO" && estado != "OBSERVADA") {
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
                            estado = estado,
                            idReceptor = doc.getString("receptorId"),
                            estadoFisico = doc.getString("estadoFisico") ?: "Usado"
                        )
                    } else null
                }
                allProducts = products
                adapter.updateList(products)
            }
    }
}
