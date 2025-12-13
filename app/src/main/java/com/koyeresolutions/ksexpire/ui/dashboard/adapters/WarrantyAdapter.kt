package com.koyeresolutions.ksexpire.ui.dashboard.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koyeresolutions.ksexpire.R
import com.koyeresolutions.ksexpire.data.entities.Item
import com.koyeresolutions.ksexpire.databinding.ItemWarrantyBinding
import com.koyeresolutions.ksexpire.utils.CurrencyUtils
import com.koyeresolutions.ksexpire.utils.DateUtils
import com.koyeresolutions.ksexpire.utils.FileUtils

/**
 * Adapter para mostrar garantías en el dashboard
 * Incluye barra de progreso de vigencia y miniatura de recibo
 */
class WarrantyAdapter(
    private val onItemClick: (Item) -> Unit,
    private val onToggleStatus: (Item) -> Unit,
    private val onDeleteClick: (Item) -> Unit
) : ListAdapter<Item, WarrantyAdapter.WarrantyViewHolder>(WarrantyDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WarrantyViewHolder {
        val binding = ItemWarrantyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WarrantyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WarrantyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WarrantyViewHolder(
        private val binding: ItemWarrantyBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            binding.apply {
                // Información básica
                textName.text = item.name
                
                // Precio (opcional)
                if (item.price != null) {
                    textPrice.text = CurrencyUtils.formatPrice(root.context, item.price)
                    textPrice.visibility = android.view.View.VISIBLE
                } else {
                    textPrice.visibility = android.view.View.GONE
                }

                // Fechas
                textPurchaseDate.text = root.context.getString(
                    R.string.purchased_on,
                    DateUtils.formatDate(item.purchaseDate)
                )

                val daysUntilExpiry = item.getDaysUntilExpiry()
                textExpiryDate.text = when {
                    daysUntilExpiry > 0 -> root.context.getString(
                        R.string.expires_in,
                        daysUntilExpiry
                    )
                    daysUntilExpiry == 0 -> "Vence hoy"
                    else -> root.context.getString(
                        R.string.expired_days_ago,
                        -daysUntilExpiry
                    )
                }

                // Barra de progreso de vigencia
                val progress = item.getWarrantyProgress()
                progressVigencia.progress = (progress * 100).toInt()

                // Colores de la barra según estado
                val warrantyStatus = item.getWarrantyStatus()
                val progressColor = when (warrantyStatus) {
                    0 -> R.color.warranty_status_expired // Rojo - Vencida
                    1 -> R.color.warranty_status_warning // Amarillo - Por vencer
                    else -> R.color.warranty_status_good // Verde - Vigente
                }
                progressVigencia.progressTintList = 
                    android.content.res.ColorStateList.valueOf(root.context.getColor(progressColor))

                // Color del texto de vencimiento
                val textColor = when (warrantyStatus) {
                    0 -> R.color.danger
                    1 -> R.color.warning
                    else -> R.color.md_theme_light_onSurfaceVariant
                }
                textExpiryDate.setTextColor(root.context.getColor(textColor))

                // Miniatura del recibo
                if (!item.imagePath.isNullOrBlank()) {
                    val bitmap = FileUtils.loadImageFromFile(root.context, item.imagePath)
                    if (bitmap != null) {
                        // Crear miniatura optimizada para la lista
                        val thumbnail = FileUtils.createThumbnail(bitmap, 120)
                        imageReceipt.setImageBitmap(thumbnail)
                        imageReceipt.visibility = android.view.View.VISIBLE
                        iconNoImage.visibility = android.view.View.GONE
                    } else {
                        showNoImageState()
                    }
                } else {
                    showNoImageState()
                }

                // Estado activo/inactivo
                val alpha = if (item.isActive) 1.0f else 0.6f
                root.alpha = alpha

                // Click listeners
                root.setOnClickListener {
                    onItemClick(item)
                }

                buttonToggleStatus.setOnClickListener {
                    onToggleStatus(item)
                }

                buttonDelete.setOnClickListener {
                    onDeleteClick(item)
                }

                // Configurar icono del botón según estado
                val toggleIcon = if (item.isActive) {
                    R.drawable.ic_pause
                } else {
                    R.drawable.ic_play
                }
                buttonToggleStatus.setIconResource(toggleIcon)

                // Animación de entrada
                root.translationY = 50f
                root.alpha = 0f
                root.animate()
                    .translationY(0f)
                    .alpha(if (item.isActive) 1f else 0.6f)
                    .setDuration(300)
                    .start()
            }
        }

        private fun showNoImageState() {
            binding.imageReceipt.visibility = android.view.View.GONE
            binding.iconNoImage.visibility = android.view.View.VISIBLE
        }
    }

    /**
     * DiffCallback para optimizar actualizaciones de la lista
     */
    private class WarrantyDiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }
    }
}