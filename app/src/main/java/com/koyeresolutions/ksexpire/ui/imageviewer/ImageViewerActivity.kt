package com.koyeresolutions.ksexpire.ui.imageviewer

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.koyeresolutions.ksexpire.databinding.ActivityImageViewerBinding
import com.koyeresolutions.ksexpire.utils.FileUtils

/**
 * Actividad para ver imagen en pantalla completa
 * Permite zoom y navegación de la imagen del recibo
 */
class ImageViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageViewerBinding

    companion object {
        const val EXTRA_IMAGE_PATH = "image_path"
        const val EXTRA_ITEM_NAME = "item_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        loadImage()
    }

    /**
     * Configurar toolbar
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        val itemName = intent.getStringExtra(EXTRA_ITEM_NAME)
        supportActionBar?.title = itemName ?: "Recibo"
    }

    /**
     * Cargar y mostrar imagen
     */
    private fun loadImage() {
        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        
        if (imagePath != null) {
            val bitmap = FileUtils.loadImageFromFile(this, imagePath)
            if (bitmap != null) {
                binding.photoView.setImageBitmap(bitmap)
            } else {
                // Mostrar error o imagen por defecto
                finish()
            }
        } else {
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}