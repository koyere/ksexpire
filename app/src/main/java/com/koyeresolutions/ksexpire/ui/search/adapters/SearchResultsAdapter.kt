package com.koyeresolutions.ksexpire.ui.search.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koyeresolutions.ksexpire.R
import com.koyeresolutions.ksexpire.data.entities.Item
import com.koyeresolutions.ksexpire.databinding.ItemSearchResultBinding
import com.koyeresolutions.ksexpire.utils.CurrencyUtils
import com.koyeresolutions.ksexpire.utils.DateUtils
import com.koyeresolutions.ksexpire.utils.FileUtils

/**
 * Adapter para resultados de búsqueda
 * Muestra ítems con información relevante y acceso rápido a imágenes
 */
class SearchResultsAdapter(
    private val onItemClick: (Item) -> Unit,
    private val onImageClick: (Item) -> Unit,
    private val onToggleStatus: (Item) -> Unit,
    private val onDeleteClick: (Item) -> Unit
) : ListAdapter<Item, SearchResultsAdapter.SearchResultViewHolder>(SearchResultDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val binding = ItemSearchResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SearchResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SearchResultViewHolder(
        private val binding: ItemSearchResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            binding.apply {
                // Información básica
                textName.text = item.name
                
                // Tipo de ítem
                val typeText = if (item.isSubscription()) {
                    root.context.getString(R.string.item_type_subscription)
                } else {
                    root.context.getString(R.string.item_type_warranty)
                }
                textType.text = typeText
                
                // Icono según tipo
                val iconRes = if (item.isSubscription()) {
                    R.drawable.ic_subscription
                } else {
                    R.drawable.ic_warranty
                }
                imageType.setImageResource(iconRes)

                // Precio
                if (item.price != null) {
                    textPrice.text = if (item.isSubscription()) {
                        CurrencyUtils.formatSubscriptionPrice(
                            root.context,
                            item.price,
                            item.billingFrequency
                        )
                    } else {
                        CurrencyUtils.formatPrice(root.context, item.price)
                    }
                    textPrice.visibility = View.VISIBLE
                } else {
                    textPrice.visibility = View.GONE
                }

                // Fechas y estado
                if (item.isSubscription()) {
                    textDateInfo.text = root.context.getString(
                        R.string.next_billing,
                        DateUtils.getRelativeDateDescription(item.expiryDate)
                    )
                } else {
                    val daysUntilExpiry = item.getDaysUntilExpiry()
                    textDateInfo.text = when {
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
                }

                // Estado de vigencia para garantías
                if (item.isWarranty()) {
                    val warrantyStatus = item.getWarrantyStatus()
                    val statusColor = when (warrantyStatus) {
                        0 -> R.color.warranty_status_expired
                        1 -> R.color.warranty_status_warning
                        else -> R.color.warranty_status_good
                    }
                    
                    indicatorStatus.setBackgroundColor(root.context.getColor(statusColor))
                    indicatorStatus.visibility = View.VISIBLE
                } else {
                    indicatorStatus.visibility = View.GONE
                }

                // Imagen del recibo
                if (!item.imagePath.isNullOrBlank()) {
                    val bitmap = FileUtils.loadImageFromFile(root.context, item.imagePath)
                    if (bitmap != null) {
                        val thumbnail = FileUtils.createThumbnail(bitmap, 80)
                        imageReceipt.setImageBitmap(thumbnail)
                        imageReceipt.visibility = View.VISIBLE
                        iconNoImage.visibility = View.GONE
                        
                        // Click en imagen para ver completa
                        imageReceipt.setOnClickListener {
                            onImageClick(item)
                        }
                    } else {
                        showNoImageState()
                    }
                } else {
                    showNoImageState()
                }

                // Estado activo/inactivo
                val alpha = if (item.isActive) 1.0f else 0.6f
                root.alpha = alpha
                
                // Texto de estado
                textStatus.text = if (item.isActive) {
                    root.context.getString(R.string.status_active)
                } else {
                    "Inactivo"
                }
                textStatus.setTextColor(
                    root.context.getColor(
                        if (item.isActive) R.color.success else R.color.warning
                    )
                )

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
                root.alpha = 0f
                root.animate()
                    .alpha(if (item.isActive) 1f else 0.6f)
                    .setDuration(200)
                    .start()
            }
        }

        private fun showNoImageState() {
            binding.imageReceipt.visibility = View.GONE
            binding.iconNoImage.visibility = View.VISIBLE
        }
    }

    /**
     * DiffCallback para optimizar actualizaciones
     */
    private class SearchResultDiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }
    }
}