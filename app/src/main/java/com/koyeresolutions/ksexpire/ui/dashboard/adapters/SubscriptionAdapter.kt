package com.koyeresolutions.ksexpire.ui.dashboard.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.koyeresolutions.ksexpire.R
import com.koyeresolutions.ksexpire.data.entities.Item
import com.koyeresolutions.ksexpire.databinding.ItemSubscriptionBinding
import com.koyeresolutions.ksexpire.utils.CurrencyUtils
import com.koyeresolutions.ksexpire.utils.DateUtils

/**
 * Adapter para mostrar suscripciones en el dashboard
 * Implementa Material Design 3 y animaciones suaves
 */
class SubscriptionAdapter(
    private val onItemClick: (Item) -> Unit,
    private val onToggleStatus: (Item) -> Unit,
    private val onDeleteClick: (Item) -> Unit
) : ListAdapter<Item, SubscriptionAdapter.SubscriptionViewHolder>(SubscriptionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriptionViewHolder {
        val binding = ItemSubscriptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SubscriptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubscriptionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SubscriptionViewHolder(
        private val binding: ItemSubscriptionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            binding.apply {
                // Información básica
                textName.text = item.name
                textPrice.text = CurrencyUtils.formatSubscriptionPrice(
                    root.context,
                    item.price,
                    item.billingFrequency
                )

                // Próximo cobro
                textNextBilling.text = root.context.getString(
                    R.string.next_billing,
                    DateUtils.getRelativeDateDescription(item.expiryDate)
                )

                // Estado
                val statusText = if (item.isActive) {
                    root.context.getString(R.string.status_active)
                } else {
                    root.context.getString(R.string.status_paused)
                }
                textStatus.text = statusText

                // Colores según estado
                val statusColor = if (item.isActive) {
                    R.color.success
                } else {
                    R.color.warning
                }
                textStatus.setTextColor(root.context.getColor(statusColor))

                // Indicador de vencimiento próximo
                val daysUntilExpiry = item.getDaysUntilExpiry()
                
                // Prueba gratuita
                if (item.isFreeTrial && item.freeTrialEndDate != null) {
                    textFreeTrialBadge.visibility = android.view.View.VISIBLE
                    val trialDaysLeft = ((item.freeTrialEndDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
                    when {
                        trialDaysLeft <= 0 -> {
                            textFreeTrialCountdown.text = root.context.getString(R.string.free_trial_expired)
                            textFreeTrialCountdown.visibility = android.view.View.VISIBLE
                        }
                        else -> {
                            textFreeTrialCountdown.text = root.context.getString(
                                R.string.free_trial_countdown,
                                com.koyeresolutions.ksexpire.utils.DateUtils.formatDate(item.freeTrialEndDate),
                                trialDaysLeft
                            )
                            textFreeTrialCountdown.visibility = android.view.View.VISIBLE
                        }
                    }
                } else {
                    textFreeTrialBadge.visibility = android.view.View.GONE
                    textFreeTrialCountdown.visibility = android.view.View.GONE
                }

                when {
                    daysUntilExpiry <= 0 -> {
                        indicatorUrgency.setBackgroundColor(root.context.getColor(R.color.danger))
                        indicatorUrgency.visibility = android.view.View.VISIBLE
                    }
                    daysUntilExpiry <= 3 -> {
                        indicatorUrgency.setBackgroundColor(root.context.getColor(R.color.warning))
                        indicatorUrgency.visibility = android.view.View.VISIBLE
                    }
                    else -> {
                        indicatorUrgency.visibility = android.view.View.GONE
                    }
                }

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

                // Configurar iconos según estado
                val toggleIcon = if (item.isActive) {
                    R.drawable.ic_pause
                } else {
                    R.drawable.ic_play
                }
                buttonToggleStatus.setIconResource(toggleIcon)

                // Categoría
                if (!item.category.isNullOrBlank()) {
                    val predefined = com.koyeresolutions.ksexpire.utils.Constants.PREDEFINED_CATEGORIES
                        .find { it.name == item.category }
                    textCategory.text = if (predefined != null) {
                        "${predefined.emoji} ${item.category}"
                    } else {
                        item.category
                    }
                    textCategory.visibility = android.view.View.VISIBLE
                } else {
                    textCategory.visibility = android.view.View.GONE
                }

                // Animación de entrada
                root.alpha = 0f
                root.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }
        }
    }

    /**
     * DiffCallback para optimizar actualizaciones de la lista
     */
    private class SubscriptionDiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }
    }
}