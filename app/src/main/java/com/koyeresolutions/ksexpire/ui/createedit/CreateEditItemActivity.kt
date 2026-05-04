package com.koyeresolutions.ksexpire.ui.createedit

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
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
import com.koyeresolutions.ksexpire.utils.CurrencyUtils
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
    private var isUpdatingUI = false

    // Launcher moderno para cámara (reemplaza startActivityForResult)
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val imagePath = data?.getStringExtra("image_path")
            if (imagePath != null) {
                viewModel.updateImagePath(imagePath)
                
                // Procesar resultados OCR si hay
                val ocrName = data.getStringExtra("ocr_name")
                val ocrPrice = if (data.hasExtra("ocr_price")) data.getDoubleExtra("ocr_price", 0.0) else null
                val ocrDate = if (data.hasExtra("ocr_date")) data.getLongExtra("ocr_date", 0L) else null
                
                if (ocrName != null || ocrPrice != null || ocrDate != null) {
                    showOcrConfirmationDialog(ocrName, ocrPrice, ocrDate)
                } else {
                    showSuccess("Foto guardada correctamente")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityCreateEditItemBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupUI()
        setupObservers()
        setupBackNavigation()
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
        setupCategorySelector()
        setupFrequencySpinner()
        setupDatePickers()
        setupQuickDurationChips()
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
     * Configurar selector de categoría con chips dinámicos
     */
    private fun setupCategorySelector() {
        val chipGroup = binding.chipGroupCategory
        chipGroup.removeAllViews()
        
        // Agregar chip "Sin categoría"
        val noneChip = com.google.android.material.chip.Chip(this).apply {
            text = "Ninguna"
            isCheckable = true
            isChecked = true
            setOnClickListener {
                viewModel.updateCategory(null, null)
            }
        }
        chipGroup.addView(noneChip)
        
        // Agregar chips de categorías predefinidas
        Constants.PREDEFINED_CATEGORIES.forEach { category ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = "${category.emoji} ${category.name}"
                isCheckable = true
                tag = category
                setOnClickListener {
                    viewModel.updateCategory(category.name, category.color)
                }
            }
            chipGroup.addView(chip)
        }
        
        // Agregar chip "Otra"
        val otherChip = com.google.android.material.chip.Chip(this).apply {
            text = getString(R.string.form_category_other)
            isCheckable = true
            setOnClickListener {
                showCustomCategoryDialog()
            }
        }
        chipGroup.addView(otherChip)
    }

    /**
     * Mostrar diálogo para categoría personalizada
     */
    private fun showCustomCategoryDialog() {
        val input = com.google.android.material.textfield.TextInputEditText(this)
        input.hint = "Nombre de la categoría"
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
        
        val container = android.widget.FrameLayout(this)
        val params = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(48, 16, 48, 0)
        input.layoutParams = params
        container.addView(input)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Categoría personalizada")
            .setView(container)
            .setPositiveButton("Aceptar") { _, _ ->
                val name = input.text?.toString()?.trim()
                if (!name.isNullOrBlank()) {
                    viewModel.updateCategory(name, "#607D8B") // Color gris por defecto
                }
            }
            .setNegativeButton("Cancelar") { _, _ ->
                // Deseleccionar el chip "Otra"
                binding.chipGroupCategory.clearCheck()
            }
            .show()
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
                // Limpiar selección de chips cuando se selecciona fecha manual
                binding.chipGroupDuration.clearCheck()
            }
        }
        
        binding.buttonExpiryDate.setOnClickListener {
            showDatePicker { timestamp ->
                viewModel.updateExpiryDate(timestamp)
                // Limpiar selección de chips cuando se selecciona fecha manual
                binding.chipGroupDuration.clearCheck()
            }
        }
    }

    /**
     * Configurar chips de duración rápida (solo para garantías)
     */
    private fun setupQuickDurationChips() {
        // Mapeo de chips a días
        val durationMap = mapOf(
            R.id.chip_30_days to 30,
            R.id.chip_60_days to 60,
            R.id.chip_90_days to 90,
            R.id.chip_6_months to 180,
            R.id.chip_12_months to 365,
            R.id.chip_24_months to 730
        )

        binding.chipGroupDuration.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val days = durationMap[checkedIds[0]] ?: return@setOnCheckedStateChangeListener
                calculateExpiryFromDuration(days)
            }
        }
    }

    /**
     * Calcular fecha de vencimiento basada en duración desde fecha de compra
     */
    private fun calculateExpiryFromDuration(days: Int) {
        val calendar = Calendar.getInstance()
        // Usar la fecha de compra actual como base
        calendar.timeInMillis = viewModel.uiState.value.purchaseDate
        calendar.add(Calendar.DAY_OF_YEAR, days)
        viewModel.updateExpiryDate(calendar.timeInMillis)
    }

    /**
     * Actualizar labels del formulario para suscripciones
     */
    private fun updateFormLabelsForSubscription() {
        // Título del toolbar
        if (!isEditMode) {
            supportActionBar?.title = "Nueva Suscripción"
        }
        // Hints contextuales
        binding.layoutName.helperText = getString(R.string.hint_name_subscription)
        binding.layoutPrice.helperText = getString(R.string.hint_price_subscription)
        // Labels de fechas
        binding.labelPurchaseDate.text = getString(R.string.form_subscription_start_date)
        binding.labelExpiryDate.text = getString(R.string.form_next_billing_date)
    }

    /**
     * Actualizar labels del formulario para garantías
     */
    private fun updateFormLabelsForWarranty() {
        // Título del toolbar
        if (!isEditMode) {
            supportActionBar?.title = "Nueva Garantía"
        }
        // Hints contextuales
        binding.layoutName.helperText = getString(R.string.hint_name_warranty)
        binding.layoutPrice.helperText = getString(R.string.hint_price_warranty)
        // Labels de fechas
        binding.labelPurchaseDate.text = getString(R.string.form_purchase_date)
        binding.labelExpiryDate.text = getString(R.string.form_warranty_expiry_date)
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
        // Campos de texto con TextWatcher
        binding.editTextName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isUpdatingUI) {
                    viewModel.updateName(s?.toString() ?: "")
                }
            }
        })
        
        binding.editTextPrice.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isUpdatingUI) {
                    viewModel.updatePrice(s?.toString() ?: "")
                }
            }
        })
        
        // Switch de estado activo
        binding.switchActive.setOnCheckedChangeListener { _, _ ->
            if (!isUpdatingUI) viewModel.toggleActiveStatus()
        }
        
        // Switch de prueba gratuita
        binding.switchFreeTrial.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUI) {
                binding.layoutFreeTrialDate.visibility = if (isChecked) View.VISIBLE else View.GONE
                if (!isChecked) {
                    viewModel.updateFreeTrial(false, null)
                } else {
                    // Fecha por defecto: 7 días desde hoy
                    val defaultEnd = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)
                    viewModel.updateFreeTrial(true, defaultEnd)
                    binding.buttonFreeTrialDate.text = DateUtils.formatDate(defaultEnd)
                }
            }
        }
        
        // Botón fecha de prueba gratuita
        binding.buttonFreeTrialDate.setOnClickListener {
            showDatePicker { timestamp ->
                viewModel.updateFreeTrial(true, timestamp)
                binding.buttonFreeTrialDate.text = DateUtils.formatDate(timestamp)
            }
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

        // Detección de duplicados
        viewModel.duplicateItem.observe(this) { duplicate ->
            if (duplicate != null) {
                showDuplicateWarning(duplicate)
            } else {
                binding.cardDuplicateWarning.visibility = View.GONE
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
        isUpdatingUI = true // Evitar que TextWatcher dispare actualizaciones
        
        // Tipo de ítem - actualizar visibilidad y textos contextuales
        when (state.itemType) {
            Constants.ITEM_TYPE_SUBSCRIPTION -> {
                binding.chipSubscription.isChecked = true
                binding.layoutFrequency.visibility = View.VISIBLE
                binding.layoutQuickDuration.visibility = View.GONE
                binding.layoutFreeTrial.visibility = View.VISIBLE
                updateFormLabelsForSubscription()
            }
            Constants.ITEM_TYPE_WARRANTY -> {
                binding.chipWarranty.isChecked = true
                binding.layoutFrequency.visibility = View.GONE
                binding.layoutQuickDuration.visibility = View.VISIBLE
                binding.layoutFreeTrial.visibility = View.GONE
                updateFormLabelsForWarranty()
            }
        }
        
        // Campos de texto - solo actualizar si es diferente para evitar mover cursor
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
        
        // Categoría - seleccionar el chip correcto
        updateCategoryChipSelection(state.category)
        
        // Prueba gratuita
        binding.switchFreeTrial.isChecked = state.isFreeTrial
        binding.layoutFreeTrialDate.visibility = if (state.isFreeTrial) View.VISIBLE else View.GONE
        if (state.freeTrialEndDate != null) {
            binding.buttonFreeTrialDate.text = DateUtils.formatDate(state.freeTrialEndDate)
        }
        
        // Imagen
        updateImageUI(state.imagePath)
        
        isUpdatingUI = false // Permitir actualizaciones de nuevo
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
     * Mostrar banner de advertencia de duplicado
     */
    private fun showDuplicateWarning(duplicate: com.koyeresolutions.ksexpire.data.entities.Item) {
        binding.cardDuplicateWarning.visibility = View.VISIBLE
        
        val message = if (duplicate.price != null) {
            getString(R.string.duplicate_warning, duplicate.name, 
                CurrencyUtils.formatSubscriptionPrice(this, duplicate.price, duplicate.billingFrequency))
        } else {
            getString(R.string.duplicate_warning_no_price, duplicate.name)
        }
        binding.textDuplicateMessage.text = message
        
        binding.buttonViewDuplicate.setOnClickListener {
            // Abrir el ítem existente
            val intent = Intent(this, com.koyeresolutions.ksexpire.ui.detail.ItemDetailActivity::class.java)
            intent.putExtra(com.koyeresolutions.ksexpire.ui.detail.ItemDetailActivity.EXTRA_ITEM_ID, duplicate.id)
            startActivity(intent)
        }
        
        binding.buttonDismissDuplicate.setOnClickListener {
            viewModel.dismissDuplicate()
        }
    }

    /**
     * Actualizar selección de chip de categoría
     */
    private fun updateCategoryChipSelection(category: String?) {
        val chipGroup = binding.chipGroupCategory
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? com.google.android.material.chip.Chip ?: continue
            val chipCategory = (chip.tag as? Constants.Category)?.name
            chip.isChecked = when {
                category == null && i == 0 -> true // "Ninguna"
                chipCategory != null && chipCategory == category -> true
                else -> false
            }
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
        intent.putExtra("enable_ocr", true)
        cameraLauncher.launch(intent)
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
                onBackPressedDispatcher.onBackPressed()
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
     * Configurar navegación hacia atrás con confirmación de cambios
     */
    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.hasChanges()) {
                    MaterialAlertDialogBuilder(this@CreateEditItemActivity)
                        .setTitle("¿Descartar cambios?")
                        .setMessage("Tienes cambios sin guardar. ¿Estás seguro de salir?")
                        .setPositiveButton("Descartar") { _, _ ->
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
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

    /**
     * Mostrar diálogo de confirmación de datos OCR
     */
    private fun showOcrConfirmationDialog(name: String?, price: Double?, date: Long?) {
        val message = buildString {
            append("Detectamos estos datos del recibo:\n\n")
            if (name != null) append("📝 Nombre: $name\n")
            if (price != null) append("💰 Precio: $${String.format("%.2f", price)}\n")
            if (date != null) append("📅 Fecha: ${DateUtils.formatDate(date)}\n")
            append("\n¿Deseas usar estos datos?")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("📷 Datos detectados")
            .setMessage(message)
            .setPositiveButton("Usar datos") { _, _ ->
                // Aplicar datos OCR al formulario
                if (name != null && viewModel.uiState.value.name.isBlank()) {
                    viewModel.updateName(name)
                    binding.editTextName.setText(name)
                }
                if (price != null && viewModel.uiState.value.price.isBlank()) {
                    viewModel.updatePrice(price.toString())
                    binding.editTextPrice.setText(String.format("%.2f", price))
                }
                if (date != null) {
                    viewModel.updatePurchaseDate(date)
                }
                showSuccess("Datos aplicados correctamente")
            }
            .setNegativeButton("Ignorar") { _, _ ->
                showSuccess("Foto guardada correctamente")
            }
            .show()
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