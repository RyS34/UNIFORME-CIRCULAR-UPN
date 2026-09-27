package com.example.uniformecircular

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import com.bumptech.glide.Glide

class MyExchangesAdapter(
    private var productList: List<Prenda>,
    private val currentUserId: String?,
    private val onItemClick: (Prenda) -> Unit,
    private val onDeleteClick: (Prenda) -> Unit,
    private val onConfirmClick: (Prenda) -> Unit,
    private val onCancelClick: (Prenda) -> Unit
) : RecyclerView.Adapter<MyExchangesAdapter.MyViewHolder>() {

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivExchangeImage: ImageView = view.findViewById(R.id.ivExchangeImage)
        val tvExchangeName: TextView = view.findViewById(R.id.tvExchangeName)
        val tvExchangeCategory: TextView = view.findViewById(R.id.tvExchangeCategory)
        val tvExchangeStatus: TextView = view.findViewById(R.id.tvExchangeStatus)
        val tvExchangePoints: TextView = view.findViewById(R.id.tvExchangePoints)
        val btnDeleteExchange: ImageButton = view.findViewById(R.id.btnDeleteExchange)
        val btnConfirmExchange: ImageButton = view.findViewById(R.id.btnConfirmExchange)
        val btnCancelProcess: ImageButton = view.findViewById(R.id.btnCancelProcess)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_exchange, parent, false)
        return MyViewHolder(view)
    }
    // Configuración de cada elemento de la lista de intercambios
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val product = productList[position]
        holder.tvExchangeName.text = product.titulo
        
        val context = holder.itemView.context
        holder.tvExchangeCategory.text = context.getString(R.string.label_category_talla_genero, product.carrera, product.talla, product.genero)
        holder.tvExchangePoints.text = context.getString(R.string.label_points, product.puntos)
        
        // Cargar imagen de forma dinámica usando Glide para mayor eficiencia
        val imageUriString = product.imagenUri
        if (!imageUriString.isNullOrEmpty()) {
            if (imageUriString.startsWith("http")) {
                Glide.with(context).load(imageUriString).into(holder.ivExchangeImage)
            } else if (imageUriString.startsWith("content://") || imageUriString.startsWith("file://")) {
                holder.ivExchangeImage.setImageURI(android.net.Uri.parse(imageUriString))
            } else {
                val imageName = imageUriString.trim().lowercase()
                val resId = when(imageName) {
                    "chompa_upn" -> R.drawable.chompa_upn
                    "pantalon_upn" -> R.drawable.pantalon_upn
                    "bata_upn" -> R.drawable.bata_upn
                    "casaca_deportiva_upn" -> R.drawable.casaca_deportiva_upn
                    else -> context.resources.getIdentifier(imageName, "drawable", context.packageName)
                }
                if (resId != 0) holder.ivExchangeImage.setImageResource(resId)
                else holder.ivExchangeImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } else {
            holder.ivExchangeImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // Estado dinámico con color y botones de acción
        val status = product.estado
        holder.tvExchangeStatus.text = status
        
        // Reset visibilities
        holder.btnDeleteExchange.visibility = View.GONE
        holder.btnConfirmExchange.visibility = View.GONE
        holder.btnCancelProcess.visibility = View.GONE
        holder.itemView.alpha = 1.0f
        // Configurar colores y visibilidad según el estado de la prenda
        when (status) {
            "CANJEADO" -> {
                holder.tvExchangeStatus.setTextColor(context.getColor(R.color.slate_500))
                holder.tvExchangeStatus.setBackgroundResource(R.drawable.bg_badge_exchanged)
                holder.itemView.alpha = 0.7f
            }
            "EN PROCESO" -> {
                holder.tvExchangeStatus.setTextColor(context.getColor(R.color.amber_600))
                holder.tvExchangeStatus.setBackgroundResource(R.drawable.bg_badge_in_progress)
                
                // Solo el DUEÑO de la prenda puede confirmar o cancelar el proceso
                if (product.idUsuario == currentUserId) {
                    holder.btnConfirmExchange.visibility = View.VISIBLE
                    holder.btnCancelProcess.visibility = View.VISIBLE
                }
            }
            "OBSERVADA" -> {
                holder.tvExchangeStatus.text = "CORREGIR"
                holder.tvExchangeStatus.setTextColor(context.getColor(R.color.amber_600))
                holder.tvExchangeStatus.setBackgroundResource(R.drawable.bg_badge_in_progress) // Reutilizar badge estilizado ámbar
                
                if (product.idUsuario == currentUserId) {
                    holder.btnDeleteExchange.visibility = View.VISIBLE
                }
            }
            else -> { // DISPONIBLE
                holder.tvExchangeStatus.setTextColor(context.getColor(R.color.emerald_500))
                holder.tvExchangeStatus.setBackgroundResource(R.drawable.bg_badge_available)
                
                // Solo el DUEÑO puede eliminar su propia publicación
                if (product.idUsuario == currentUserId) {
                    holder.btnDeleteExchange.visibility = View.VISIBLE
                }
            }
        }
        // Configurar listeners para los botones
        holder.itemView.setOnClickListener { onItemClick(product) }
        holder.btnDeleteExchange.setOnClickListener { onDeleteClick(product) }
        holder.btnConfirmExchange.setOnClickListener { onConfirmClick(product) }
        holder.btnCancelProcess.setOnClickListener { onCancelClick(product) }
    }
    // Tamaño de la lista de intercambios en el RecyclerView
    override fun getItemCount(): Int = productList.size

    // Actualizar la lista de intercambios en el adaptador
    fun updateList(newList: List<Prenda>) {
        productList = newList
        notifyDataSetChanged()
    }
}
