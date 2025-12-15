package com.koyeresolutions.ksexpire.ui.createedit

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.koyeresolutions.ksexpire.R
import com.koyeresolutions.ksexpire.databinding.ActivityCreateEditItemBinding
import com.koyeresolutions.ksexpire.ui.about.AboutViewModel
import com.koyeresolutions.ksexpire.ui.camera.CameraActivity
import com.koyeresolutions.ksexpire.utils.Constants
import com.koyeresolutions.ksexpire.utils.DateUtils
import com.koyeresolutions.ksexpire.utils.FileUtils
import kotlinx.coroutines.launch
import java.util.*

/**
 * Actividad para crear y editar ítems
 * Implementa formulario completo con validaciones y cámara
 */
class CreateEditItemActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateEditItemBinding
    private val viewModel: CreateEditItemViewModel by viewModels()
    private val aboutViewModel: AboutViewModel by viewModels()

    private var isEditMode = false

    companion object {
        private const val REQUEST_CAMERA = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityCreateEditItemBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupUI()
        setupObservers()
        initializeViewModel()
    }

    /**
     * Configurar toolbar
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    /**
     * Configurar elementos de UI
     */
    private fun setupUI() {
        setupTypeSelector()
        setupFrequencySpinner()
        setupDatePickers()
        setupImageSection()
        setupClickListeners()
    }

    /**
     * Configurar selector de tipo de ítem
     */
    private fun setupTypeSelector() {
        binding.chipGroupType.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val selectedType = when (checkedIds[0]) {
                    R.id.chip_subscription -> Constants.ITEM_TYPE_SUBSCRIPTION
                    R.id.chip_warranty -> Constants.ITEM_TYPE_WARRANTY
                    else -> Constants.ITEM_TYPE_SUBSCRIPTION
                }
                viewModel.updateItemType(selectedType)
            }
        }
    }

    /**
     * Configurar spinner de frecuencia
     */
    private fun setupFrequencySpinner() {
        val frequencies = arrayOf(
            getString(R.string.frequency_weekly),
            getString(R.string.frequency_monthly),
            getString(R.string.frequency_annual)
        )
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, frequencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFrequency.adapter = adapter
        
        binding.spinnerFrequency.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val frequency = when (position) {
                    0 -> Constants.FREQUENCY_WEEKLY
                    1 -> Constants.FREQUENCY_MONTHLY
                    2 -> Constants.FREQUENCY_ANNUAL
                    else -> Constants.FREQUENCY_MONTHLY
                }
                viewModel.updateBillingFrequency(frequency)
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
    }

    /**
     * Configurar selectores de fecha
     */
    private fun setupDatePickers() {
        binding.buttonPurchaseDate.setOnClickListener {
            showDatePicker { timestamp ->
                viewModel.updatePurchaseDate(timestamp)
            }
        }
        
        binding.buttonExpiryDate.setOnClickListener {
            showDatePicker { timestamp ->
                viewModel.updateExpiryDate(timestamp)
            }
        }
    }

    /**
     * Configurar sección de imagen
     */
    private fun setupImageSection() {
        binding.buttonTakePhoto.setOnClickListener {
            openCamera()
        }
        
        binding.buttonRemovePhoto.setOnClickListener {
            viewModel.updateImagePath(null)
        }
    }

    /**
     * Configurar listeners de clicks
     */
    private fun setupClickListeners() {
        // Campos de texto
        binding.editTextName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                viewModel.updateName(binding.editTextName.text.toString())
            }
        }
        
        binding.editTextPrice.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                viewModel.updatePrice(binding.editTextPrice.text.toString())
            }
        }
        
        // Switch de estado activo
        binding.switchActive.setOnCheckedChangeListener { _, _ ->
            viewModel.toggleActiveStatus()
        }
        
        // Botón Guardar
        binding.buttonSave.setOnClickListener {
            saveItem()
        }
    }

    /**
     * Configurar observadores
     */
    private fun setupObservers() {
        // Estado del UI
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUI(state)
            }
        }
        
        // Estado de carga
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.buttonSave.isEnabled = !isLoading
        }
        
        // Errores de validación
        viewModel.validationErrors.observe(this) { errors ->
            if (errors.isNotEmpty()) {
                showValidationErrors(errors)
            }
        }
        
        // Resultado de guardado
        viewModel.saveResult.observe(this) { result ->
            when (result) {
                is CreateEditItemViewModel.SaveResult.Success -> {
                    showSuccess()
                    
                    // TRIGGER AUTOMÁTICO DE REVIEW SEGÚN EL PLANNING
                    // Después del 3er ítem creado (momento de satisfacción)
                    if (!isEditMode) {
                        aboutViewModel.incrementItemsCreated()
                        
                        if (aboutViewModel.checkAutoReviewTrigger()) {
                            // Mostrar review automáticamente
                            aboutViewModel.requestInAppReview(this)
                        }
                    }
                    
                    finish()
                }
                is CreateEditItemViewModel.SaveResult.Error -> {
                    showError(result.message)
                }
                null -> {} // No hacer nada
            }
        }
    }

    /**
     * Inicializar ViewModel según el modo
     */
    private fun initializeViewModel() {
        val itemId = intent.getLongExtra("item_id", -1L)
        val preselectedType = intent.getStringExtra("preselected_type")
        
        if (itemId != -1L) {
            // Modo edición
            isEditMode = true
            supportActionBar?.title = "Editar ítem"
            viewModel.initializeForEdit(itemId)
        } else {
            // Modo creación
            isEditMode = false
            supportActionBar?.title = "Nuevo ítem"
            viewModel.initializeForCreate(preselectedType)
        }
    }

    /**
     * Actualizar UI según el estado
     */
    private fun updateUI(state: CreateEditItemViewModel.CreateEditUiState) {
        // Tipo de ítem
        when (state.itemType) {
            Constants.ITEM_TYPE_SUBSCRIPTION -> {
                binding.chipSubscription.isChecked = true
                binding.layoutFrequency.visibility = View.VISIBLE
            }
            Constants.ITEM_TYPE_WARRANTY -> {
                binding.chipWarranty.isChecked = true
                binding.layoutFrequency.visibility = View.GONE
            }
        }
        
        // Campos de texto
        if (binding.editTextName.text.toString() != state.name) {
            binding.editTextName.setText(state.name)
        }
        
        if (binding.editTextPrice.text.toString() != state.price) {
            binding.editTextPrice.setText(state.price)
        }
        
        // Fechas
        binding.buttonPurchaseDate.text = DateUtils.formatDate(state.purchaseDate)
        binding.buttonExpiryDate.text = DateUtils.formatDate(state.expiryDate)
        
        // Frecuencia
        state.billingFrequency?.let { frequency ->
            val position = when (frequency) {
                Constants.FREQUENCY_WEEKLY -> 0
                Constants.FREQUENCY_MONTHLY -> 1
                Constants.FREQUENCY_ANNUAL -> 2
                else -> 1
            }
            binding.spinnerFrequency.setSelection(position)
        }
        
        // Estado activo
        binding.switchActive.isChecked = state.isActive
        
        // Imagen
        updateImageUI(state.imagePath)
    }

    /**
     * Actualizar UI de imagen
     */
    private fun updateImageUI(imagePath: String?) {
        if (imagePath != null) {
            val bitmap = FileUtils.loadImageFromFile(this, imagePath)
            if (bitmap != null) {
                binding.imagePreview.setImageBitmap(bitmap)
                binding.imagePreview.visibility = View.VISIBLE
                binding.buttonRemovePhoto.visibility = View.VISIBLE
                binding.buttonTakePhoto.text = "Cambiar foto"
                binding.textPhotoHint.visibility = View.GONE
            } else {
                showNoImageState()
            }
        } else {
            showNoImageState()
        }
    }

    /**
     * Mostrar estado sin imagen
     */
    private fun showNoImageState() {
        binding.imagePreview.visibility = View.GONE
        binding.buttonRemovePhoto.visibility = View.GONE
        binding.buttonTakePhoto.text = getString(R.string.form_take_photo)
        binding.textPhotoHint.visibility = View.VISIBLE
    }

    /**
     * Mostrar selector de fecha
     */
    private fun showDatePicker(onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                onDateSelected(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /**
     * Abrir cámara
     */
    private fun openCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivityForResult(intent, REQUEST_CAMERA)
    }

    /**
     * Mostrar errores de validación
     */
    private fun showValidationErrors(errors: List<String>) {
        val message = errors.joinToString("\n• ", "• ")
        MaterialAlertDialogBuilder(this)
            .setTitle("Errores de validación")
            .setMessage(message)
            .setPositiveButton("Aceptar", null)
            .show()
    }

    /**
     * Mostrar mensaje de éxito
     */
    private fun showSuccess(customMessage: String? = null) {
        val message = customMessage ?: if (isEditMode) "Ítem actualizado correctamente" else "Ítem creado correctamente"
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Mostrar mensaje de error
     */
    private fun showError(message: String) {
        Snackbar.make(binding.root, "Error: $message", Snackbar.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_create_edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_save -> {
                saveItem()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Guardar ítem
     */
    private fun saveItem() {
        // Actualizar campos antes de guardar
        viewModel.updateName(binding.editTextName.text.toString())
        viewModel.updatePrice(binding.editTextPrice.text.toString())
        
        viewModel.saveItem()
    }

    override fun onBackPressed() {
        if (viewModel.hasChanges()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("¿Descartar cambios?")
                .setMessage("Tienes cambios sin guardar. ¿Estás seguro de salir?")
                .setPositiveButton("Descartar") { _, _ ->
                    super.onBackPressed()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            val imagePath = data?.getStringExtra("image_path")
            if (imagePath != null) {
                viewModel.updateImagePath(imagePath)
                showSuccess("Foto guardada correctamente")
            }
        } else if (requestCode == REQUEST_CAMERA && resultCode == RESULT_CANCELED) {
            // Usuario canceló la captura, no hacer nada
        }
    }

    /**
     * Mostrar preview mejorado de imagen
     */
    private fun showImagePreview(imagePath: String) {
        val bitmap = FileUtils.loadImageFromFile(this, imagePath)
        if (bitmap != null) {
            // Crear miniatura para preview
            val thumbnail = FileUtils.createThumbnail(bitmap, 300)
            binding.imagePreview.setImageBitmap(thumbnail)
            binding.imagePreview.visibility = View.VISIBLE
            binding.buttonRemovePhoto.visibility = View.VISIBLE
            binding.buttonTakePhoto.text = getString(R.string.form_retake_photo)
            binding.textPhotoHint.visibility = View.GONE
        }
    }
}