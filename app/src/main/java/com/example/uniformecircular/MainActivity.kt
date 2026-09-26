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

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper
    private lateinit var adapter: ProductAdapter
    private var allProducts: List<Prenda> = listOf()

    private var currentUserId: Int = -1
    private var currentUserName: String = "Usuario"

    private fun Int.toPx(context: android.content.Context): Int = (this * context.resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DBHelper(this)

        // Ajustar insets para que el fondo sea total
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            findViewById<View>(R.id.linearLayoutTop)?.setPadding(24.toPx(this), systemBars.top + 10.toPx(this), 24.toPx(this), 10.toPx(this))
            insets
        }

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val etSearch = findViewById<EditText>(R.id.etMainSearch)
        val imgAvatarMain = findViewById<View>(R.id.imgAvatarMain)

        val username = intent.getStringExtra("USER_NAME") ?: "Usuario"
        currentUserName = username
        currentUserId = dbHelper.obtenerUsuarioId(username)
        tvWelcome.text = username
        actualizarPuntosVista()

        setupRecyclerView()
        setupSearch(etSearch)

        // Perfil Listener
        imgAvatarMain?.setOnClickListener {
            val intent = Intent(this, AccountActivity::class.java).apply {
                putExtra("USER_NAME", username)
            }
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_right, R.anim.slide_out_left)
            startActivity(intent, options.toBundle())
        }

        // Menu Listeners
        findViewById<View>(R.id.menuCatalog)?.setOnClickListener {
            val intent = Intent(this, CatalogActivity::class.java).apply {
                putExtra("USER_NAME", username)
            }
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_right, R.anim.slide_out_left)
            startActivity(intent, options.toBundle())
        }
        findViewById<View>(R.id.menuDeliver)?.setOnClickListener {
            val intent = Intent(this, DeliverActivity::class.java).apply {
                putExtra("USER_NAME", username)
            }
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_right, R.anim.slide_out_left)
            startActivity(intent, options.toBundle())
        }
        findViewById<View>(R.id.menuExchanges)?.setOnClickListener {
            val intent = Intent(this, ExchangesActivity::class.java).apply {
                putExtra("USER_NAME", username)
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
            mostrarResumenImpacto(username)
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
    private fun mostrarResumenImpacto(username: String) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.dialog_impact_summary, null)

        val rvContributions = view.findViewById<RecyclerView>(R.id.rvImpactContributions)
        val btnManage = view.findViewById<Button>(R.id.btnViewFullExchanges)
        val tvDesc = view.findViewById<TextView>(R.id.tvImpactDialogDesc)
        val tvStatAportes = view.findViewById<TextView>(R.id.tvStatAportes)
        val tvStatPuntos = view.findViewById<TextView>(R.id.tvStatPuntos)

        // Obtener prendas del usuario
        val misPrendas = dbHelper.obtenerPrendasPorUsuario(currentUserId)
        
        // Calcular puntos totales generados por sus prendas
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
        // Usamos el adaptador, pero indicando que use el layout compacto si fuera necesario,
        // o simplemente confiamos en que al ser un BottomSheet el RecyclerView se ajustará.
        // Dado que ProductAdapter infla R.layout.item_product, vamos a crear uno específico o modificarlo.
        
        val impactAdapter = ProductAdapter(misPrendas, isCompact = true) { prenda ->
            if (prenda.estado == "OBSERVADA") {
                // Si está en observación, redirigir directamente a Mis Aportes para que la corrija
                val intent = Intent(this, ExchangesActivity::class.java).apply {
                    putExtra("USER_NAME", username)
                    putExtra("SELECT_TAB", 0)
                }
                startActivity(intent)
                android.widget.Toast.makeText(this, "Esta publicación requiere corrección. Revisa los detalles.", android.widget.Toast.LENGTH_LONG).show()
            } else {
                val intent = Intent(this, CatalogActivity::class.java).apply {
                    putExtra("PRENDA_ID", prenda.id)
                    putExtra("USER_NAME", currentUserName)
                }
                startActivity(intent)
            }
            dialog.dismiss()
        }
        
        // MODIFICACIÓN: Para el diálogo de impacto, usamos una versión más pequeña si es posible.
        // Como no queremos cambiar el ProductAdapter original para no romper el Home, 
        // vamos a sobrecargar el adapter o usar uno específico para el impacto.

        rvContributions.adapter = impactAdapter

        btnManage.setOnClickListener {
            val intent = Intent(this, ExchangesActivity::class.java).apply {
                putExtra("USER_NAME", username)
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
        val username = intent.getStringExtra("USER_NAME") ?: "Usuario"
        adapter = ProductAdapter(emptyList(), isCompact = false) { prenda ->
            // Abrir catálogo con el producto seleccionado (o mostrar detalle directo)
            val intent = Intent(this, CatalogActivity::class.java).apply {
                putExtra("PRENDA_ID", prenda.id)
                putExtra("USER_NAME", username)
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
        if (currentUserId != -1) {
            val puntos = dbHelper.obtenerPuntosUsuario(currentUserId)
            findViewById<TextView>(R.id.tvPointsCount)?.text = "$puntos Pts"

            // Actualizar impacto circular (conteo de aportes)
            val conteoAportes = dbHelper.obtenerConteoAportes(currentUserId)
            val tvImpactDesc = findViewById<TextView>(R.id.tvImpactDesc)
            
            if (conteoAportes == 1) {
                tvImpactDesc.text = "1 Uniforme reciclado"
            } else {
                tvImpactDesc.text = "$conteoAportes Uniformes reciclados"
            }
        }
    }
    // Cargamos las prendas recientes del catálogo y las mostramos en el RecyclerView
    private fun loadRecentProducts() {
        val cursor = dbHelper.obtenerCatalogo()
        val products = mutableListOf<Prenda>()
        if (cursor.moveToFirst()) {
            do {
                val estado = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_ESTADO))
                
                // MEJORA 3: Solo agregar a "Recién Llegados" si NO ha sido canjeada y NO está observada
                if (estado != "CANJEADO" && estado != "OBSERVADA") {
                    products.add(Prenda(
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
                        estado,
                        if (cursor.isNull(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_ID_RECEPTOR))) null
                        else cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_ID_RECEPTOR)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_PRENDA_ESTADO_FISICO))
                    ))
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        
        // Mostrar los más recientes primero (los que tengan ID más altos aparecerán al inicio de la lista horizontal)
        val recentProducts = products.take(10)
        
        allProducts = products
        adapter.updateList(recentProducts)
    }
}
