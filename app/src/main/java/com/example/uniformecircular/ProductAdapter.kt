package com.example.uniformecircular

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ProductAdapter(
    private var productList: List<Prenda>,
    private val isCompact: Boolean = false,
    private val onProductClick: (Prenda) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProductImage: ImageView = view.findViewById(R.id.ivProductImage)
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvProductCategory: TextView = view.findViewById(R.id.tvProductCategory)
        val tvProductPoints: TextView = view.findViewById(R.id.tvProductPoints)
        val btnAdd: ImageButton? = view.findViewById(R.id.btnAdd)
    }
    // Crear una nueva vista para cada elemento de la lista de productos
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val layoutRes = when {
            isCompact -> R.layout.item_product_compact
            parent.id == R.id.rvRecentProducts -> R.layout.item_product_card
            else -> R.layout.item_product
        }
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutRes, parent, false)
        return ProductViewHolder(view)
    }
    // Enlazar los datos del producto con la vista correspondiente en cada posición
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]
        
        val context = holder.itemView.context
        if (product.estado == "OBSERVADA") {
            holder.tvProductName.text = "⚠️ [POR CORREGIR] ${product.titulo}"
            holder.tvProductName.setTextColor(context.getColor(R.color.amber_600))
        } else {
            holder.tvProductName.text = product.titulo
            holder.tvProductName.setTextColor(context.getColor(R.color.slate_800))
        }

        holder.tvProductCategory.text = context.getString(R.string.label_category_talla_genero, product.carrera, product.talla, product.genero)
        holder.tvProductPoints.text = context.getString(R.string.label_points, product.puntos)
        
        // Cargar imagen dinámicamente
        val imageUriString = product.imagenUri
        
        if (!imageUriString.isNullOrEmpty()) {
            when {
                imageUriString.startsWith("base64:") -> {
                    try {
                        val base64String = imageUriString.substring(7)
                        val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                        val decodedImage = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        holder.ivProductImage.setImageBitmap(decodedImage)
                        holder.ivProductImage.scaleType = ImageView.ScaleType.CENTER_CROP
                    } catch (e: Exception) {
                        holder.ivProductImage.setImageResource(android.R.drawable.ic_menu_gallery)
                    }
                }
                imageUriString.startsWith("http") -> {
                    Glide.with(context).load(imageUriString).into(holder.ivProductImage)
                }
                imageUriString.startsWith("content://") || imageUriString.startsWith("file://") -> {
                    holder.ivProductImage.setImageURI(android.net.Uri.parse(imageUriString))
                }
                else -> {
                    val imageName = imageUriString.trim().lowercase()
                    val resId = when(imageName) {
                        "chompa_upn" -> R.drawable.chompa_upn
                        "pantalon_upn" -> R.drawable.pantalon_upn
                        "bata_upn" -> R.drawable.bata_upn
                        "casaca_deportiva_upn" -> R.drawable.casaca_deportiva_upn
                        else -> context.resources.getIdentifier(imageName, "drawable", context.packageName)
                    }
                    if (resId != 0) holder.ivProductImage.setImageResource(resId)
                    else holder.ivProductImage.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            }
        } else {
            holder.ivProductImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // Acción al hacer clic en la tarjeta o en el botón +
        holder.itemView.setOnClickListener { onProductClick(product) }
        holder.btnAdd?.setOnClickListener { onProductClick(product) }
    }
    // Obtener el número total de elementos en la lista de productos
    override fun getItemCount(): Int = productList.size

    fun updateList(newList: List<Prenda>) {
        productList = newList
        notifyDataSetChanged()
    }
}
