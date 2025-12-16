package com.koyeresolutions.ksexpire.ui.detail

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.koyeresolutions.ksexpire.KSExpireApplication
import com.koyeresolutions.ksexpire.R
import com.koyeresolutions.ksexpire.data.entities.Item
import com.koyeresolutions.ksexpire.databinding.ActivityItemDetailBinding
import com.koyeresolutions.ksexpire.ui.createedit.CreateEditItemActivity
import com.koyeresolutions.ksexpire.ui.imageviewer.ImageViewerActivity
import com.koyeresolutions.ksexpire.utils.Constants
import com.koyeresolutions.ksexpire.utils.CurrencyUtils
import com.koyeresolutions.ksexpire.utils.DateUtils
import com.koyeresolutions.ksexpire.utils.FileUtils
import kotlinx.coroutines.launch

/**
 * Actividad para ver el detalle de un ítem sin editarlo
 */
class ItemDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemDetailBinding
    private var itemId: Long = -1
    private var currentItem: Item? = null

    companion object {
        const val EXTRA_ITEM_ID = "item_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1)
        if (itemId == -1L) {
            finish()
            return
        }

        setupToolbar()
        setupClickListeners()
        loadItem()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupClickListeners() {
        binding.buttonEdit.setOnClickListener {
            openEditItem()
        }

        binding.buttonViewFullImage.setOnClickListener {
            currentItem?.imagePath?.let { path ->
                openImageViewer(path)
            }
        }

        binding.imageReceipt.setOnClickListener {
            currentItem?.imagePath?.let { path ->
                openImageViewer(path)
            }
        }
    }

    private fun loadItem() {
        val repository = (application as KSExpireApplication).repository
        
        lifecycleScope.launch {
            val item = repository.getItemById(itemId)
            if (item != null) {
                currentItem = item
                displayItem(item)
            } else {
                finish()
            }
        }
    }

    private fun displayItem(item: Item) {
        binding.apply {
            // Nombre y tipo
            textName.text = item.name
            
            if (item.isSubscription()) {
                textType.text = getString(R.string.item_type_subscription)
                imageType.setImageResource(R.drawable.ic_subscription)
                toolbar.title = "Detalle de Suscripción"
                labelPurchaseDate.text = "Fecha de inicio"
                labelExpiryDate.text = "Próximo cobro"
                layoutFrequency.visibility = View.VISIBLE
            } else {
                textType.text = getString(R.string.item_type_warranty)
                imageType.setImageResource(R.drawable.ic_warranty)
                toolbar.title = "Detalle de Garantía"
                labelPurchaseDate.text = "Fecha de compra"
                labelExpiryDate.text = "Vencimiento de garantía"
                layoutFrequency.visibility = View.GONE
            }

            // Precio
            if (item.price != null) {
                layoutPrice.visibility = View.VISIBLE
                textPrice.text = if (item.isSubscription()) {
                    CurrencyUtils.formatSubscriptionPrice(this@ItemDetailActivity, item.price, item.billingFrequency)
                } else {
                    CurrencyUtils.formatPrice(this@ItemDetailActivity, item.price)
                }
            } else {
                layoutPrice.visibility = View.GONE
            }

            // Frecuencia
            item.billingFrequency?.let { freq ->
                textFrequency.text = when (freq) {
                    Constants.FREQUENCY_WEEKLY -> getString(R.string.frequency_weekly)
                    Constants.FREQUENCY_MONTHLY -> getString(R.string.frequency_monthly)
                    Constants.FREQUENCY_ANNUAL -> getString(R.string.frequency_annual)
                    else -> freq
                }
            }

            // Fechas
            textPurchaseDate.text = DateUtils.formatDate(item.purchaseDate)
            textExpiryDate.text = DateUtils.formatDate(item.expiryDate)

            // Días restantes
            val daysRemaining = item.getDaysUntilExpiry()
            when {
                daysRemaining > 30 -> {
                    textDaysRemaining.text = "$daysRemaining días"
                    textDaysRemaining.setTextColor(getColor(R.color.success))
                }
                daysRemaining > 0 -> {
                    textDaysRemaining.text = "$daysRemaining días"
                    textDaysRemaining.setTextColor(getColor(R.color.warning))
                }
                daysRemaining == 0 -> {
                    textDaysRemaining.text = "Vence hoy"
                    textDaysRemaining.setTextColor(getColor(R.color.danger))
                }
                else -> {
                    textDaysRemaining.text = "Vencido hace ${-daysRemaining} días"
                    textDaysRemaining.setTextColor(getColor(R.color.danger))
                }
            }

            // Estado
            if (item.isActive) {
                textStatus.text = getString(R.string.status_active)
                textStatus.setTextColor(getColor(R.color.success))
            } else {
                textStatus.text = "Inactivo"
                textStatus.setTextColor(getColor(R.color.warning))
            }

            // Imagen
            if (!item.imagePath.isNullOrBlank()) {
                val bitmap = FileUtils.loadImageFromFile(this@ItemDetailActivity, item.imagePath)
                if (bitmap != null) {
                    cardImage.visibility = View.VISIBLE
                    imageReceipt.setImageBitmap(bitmap)
                } else {
                    cardImage.visibility = View.GONE
                }
            } else {
                cardImage.visibility = View.GONE
            }
        }
    }

    private fun openEditItem() {
        val intent = Intent(this, CreateEditItemActivity::class.java)
        intent.putExtra("item_id", itemId)
        startActivity(intent)
    }

    private fun openImageViewer(imagePath: String) {
        val intent = Intent(this, ImageViewerActivity::class.java)
        intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_PATH, imagePath)
        intent.putExtra(ImageViewerActivity.EXTRA_ITEM_NAME, currentItem?.name ?: "")
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos por si se editó
        if (itemId != -1L) {
            loadItem()
        }
    }
}
