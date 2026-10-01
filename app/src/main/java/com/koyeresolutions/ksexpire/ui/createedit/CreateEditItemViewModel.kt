package com.koyeresolutions.ksexpire.ui.createedit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.koyeresolutions.ksexpire.KSExpireApplication
import com.koyeresolutions.ksexpire.data.entities.Item
import com.koyeresolutions.ksexpire.data.repository.ItemRepository
import com.koyeresolutions.ksexpire.utils.Constants
import com.koyeresolutions.ksexpire.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para crear y editar ítems
 * Maneja validación, estado del formulario y operaciones CRUD
 */
class CreateEditItemViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ItemRepository = (application as KSExpireApplication).repository

    // Estados del formulario
    private val _uiState = MutableStateFlow(CreateEditUiState())
    val uiState: StateFlow<CreateEditUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Errores del formulario completo: solo se emiten al intentar guardar (se muestran en diálogo)
    private val _validationErrors = MutableLiveData<List<String>>()
    val validationErrors: LiveData<List<String>> = _validationErrors

    // Errores por campo en tiempo real (se muestran bajo cada campo)
    private val _fieldErrors = MutableStateFlow(FieldErrors())
    val fieldErrors: StateFlow<FieldErrors> = _fieldErrors.asStateFlow()

    // Campos que el usuario ya editó: no marcar error en campos que aún no toca
    private val touchedFields = mutableSetOf<String>()
    private var saveAttempted = false

    private val _duplicateItem = MutableLiveData<Item?>()
    val duplicateItem: LiveData<Item?> = _duplicateItem

    private val _saveResult = MutableLiveData<SaveResult?>()
    val saveResult: LiveData<SaveResult?> = _saveResult

    // Ítem actual (null para crear nuevo, objeto para editar)
    private var currentItem: Item? = null
    private var isEditMode = false

    /**
     * Inicializar para crear nuevo ítem
     */
    fun initializeForCreate(preselectedType: String? = null) {
        isEditMode = false
        currentItem = null
        
        val itemType = when (preselectedType) {
            "subscription" -> Constants.ITEM_TYPE_SUBSCRIPTION
            "warranty" -> Constants.ITEM_TYPE_WARRANTY
            else -> Constants.ITEM_TYPE_SUBSCRIPTION // Por defecto suscripción
        }
        
        _uiState.value = CreateEditUiState(
            itemType = itemType,
            isEditMode = false
        )
    }

    /**
     * Inicializar para editar ítem existente
     */
    fun initializeForEdit(itemId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val item = repository.getItemById(itemId)
                if (item != null) {
                    currentItem = item
                    isEditMode = true
                    
                    _uiState.value = CreateEditUiState(
                        itemType = item.type,
                        name = item.name,
                        price = item.price?.toString() ?: "",
                        purchaseDate = item.purchaseDate,
                        expiryDate = item.expiryDate,
                        billingFrequency = item.billingFrequency ?: Constants.FREQUENCY_MONTHLY,
                        imagePath = item.imagePath,
                        isActive = item.isActive,
                        isEditMode = true,
                        category = item.category,
                        categoryColor = item.categoryColor,
                        isFreeTrial = item.isFreeTrial,
                        freeTrialEndDate = item.freeTrialEndDate
                    )
                } else {
                    _saveResult.value = SaveResult.Error("Ítem no encontrado")
                }
            } catch (e: Exception) {
                _saveResult.value = SaveResult.Error("Error al cargar ítem: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Actualizar tipo de ítem
     */
    fun updateItemType(type: Int) {
        val isSubscription = type == Constants.ITEM_TYPE_SUBSCRIPTION
        val state = _uiState.value
        _uiState.value = state.copy(
            itemType = type,
            billingFrequency = if (isSubscription) {
                state.billingFrequency ?: Constants.FREQUENCY_MONTHLY
            } else {
                null
            },
            // La prueba gratuita solo aplica a suscripciones
            isFreeTrial = isSubscription && state.isFreeTrial,
            freeTrialEndDate = if (isSubscription) state.freeTrialEndDate else null
        )
    }

    /**
     * Actualizar nombre con validación en tiempo real
     */
    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
        touchedFields += FIELD_NAME
        validateField()
        checkForDuplicates(name)
    }

    /**
     * Verificar si hay ítems duplicados con nombre similar
     */
    private fun checkForDuplicates(name: String) {
        if (name.length < 3) {
            _duplicateItem.value = null
            return
        }
        viewModelScope.launch {
            try {
                val allItems = repository.getAllActiveItemsList()
                val currentId = if (isEditMode) currentItem?.id else null
                val similar = com.koyeresolutions.ksexpire.utils.DuplicateDetector
                    .findSimilarItems(name, allItems, currentId)
                _duplicateItem.value = similar.firstOrNull()
            } catch (e: Exception) {
                _duplicateItem.value = null
            }
        }
    }

    /**
     * Descartar alerta de duplicado
     */
    fun dismissDuplicate() {
        _duplicateItem.value = null
    }

    /**
     * Actualizar precio con validación en tiempo real
     */
    fun updatePrice(price: String) {
        _uiState.value = _uiState.value.copy(price = price)
        touchedFields += FIELD_PRICE
        validateField()
    }

    /**
     * Actualizar fecha de compra con validación en tiempo real
     */
    fun updatePurchaseDate(timestamp: Long) {
        _uiState.value = _uiState.value.copy(purchaseDate = timestamp)
        validateField()
    }

    /**
     * Actualizar fecha de vencimiento con validación en tiempo real
     */
    fun updateExpiryDate(timestamp: Long) {
        _uiState.value = _uiState.value.copy(expiryDate = timestamp)
        validateField()
    }

    /**
     * Actualizar frecuencia de facturación
     */
    fun updateBillingFrequency(frequency: String) {
        _uiState.value = _uiState.value.copy(billingFrequency = frequency)
    }

    /**
     * Actualizar ruta de imagen
     */
    fun updateImagePath(imagePath: String?) {
        _uiState.value = _uiState.value.copy(imagePath = imagePath)
    }

    /**
     * Actualizar categoría
     */
    fun updateCategory(category: String?, color: String?) {
        _uiState.value = _uiState.value.copy(category = category, categoryColor = color)
    }

    /**
     * Actualizar estado de prueba gratuita
     */
    fun updateFreeTrial(isFreeTrial: Boolean, endDate: Long? = null) {
        _uiState.value = _uiState.value.copy(
            isFreeTrial = isFreeTrial,
            freeTrialEndDate = endDate
        )
    }

    /**
     * Alternar estado activo
     */
    fun toggleActiveStatus() {
        _uiState.value = _uiState.value.copy(isActive = !_uiState.value.isActive)
    }

    /**
     * Validar formulario
     */
    private fun validateForm(): List<String> {
        val errors = mutableListOf<String>()
        val state = _uiState.value
        val isSubscription = state.itemType == Constants.ITEM_TYPE_SUBSCRIPTION

        validateName(state.name)?.let { errors.add(it) }
        validatePrice(state.price)?.let { errors.add(it) }

        // Validar fechas (se comparan por día, no por hora)
        if (state.purchaseDate <= 0) {
            errors.add("La fecha de compra es obligatoria")
        }

        if (state.expiryDate <= 0) {
            errors.add("La fecha de vencimiento es obligatoria")
        }

        val purchaseDay = DateUtils.getStartOfDay(state.purchaseDate)
        val expiryDay = DateUtils.getStartOfDay(state.expiryDate)

        if (state.purchaseDate > 0 && state.expiryDate > 0 && expiryDay <= purchaseDay) {
            errors.add(
                if (isSubscription) "El próximo cobro debe ser posterior a la fecha de inicio"
                else "El vencimiento de la garantía debe ser posterior a la fecha de compra"
            )
        }

        if (!isSubscription && state.purchaseDate > DateUtils.getTodayEnd()) {
            errors.add("La fecha de compra no puede ser futura")
        }

        // Validar frecuencia para suscripciones
        if (isSubscription && state.billingFrequency.isNullOrBlank()) {
            errors.add("La frecuencia de cobro es obligatoria para suscripciones")
        }

        // Validar prueba gratuita
        if (isSubscription && state.isFreeTrial) {
            val trialEnd = state.freeTrialEndDate
            if (trialEnd == null) {
                errors.add("Indica cuándo termina la prueba gratuita")
            } else if (DateUtils.getStartOfDay(trialEnd) < purchaseDay) {
                errors.add("La prueba gratuita no puede terminar antes de la fecha de inicio")
            } else if (!isEditMode && trialEnd < DateUtils.getTodayStart()) {
                errors.add("La prueba gratuita ya terminó; desactívala o elige otra fecha")
            }
        }

        return errors
    }

    private fun validateName(name: String): String? = when {
        name.isBlank() -> "El nombre es obligatorio"
        name.trim().length > MAX_NAME_LENGTH -> "El nombre no puede superar $MAX_NAME_LENGTH caracteres"
        else -> null
    }

    private fun validatePrice(price: String): String? {
        if (price.isBlank()) return null // El precio es opcional
        val value = parsePrice(price) ?: return "Ingresa un precio válido (ej. 199.99)"
        val decimals = price.trim().replace(',', '.').substringAfter('.', "")
        return when {
            value < 0 -> "El precio no puede ser negativo"
            value > MAX_PRICE -> "El precio es demasiado alto"
            decimals.length > 2 -> "Usa como máximo 2 decimales"
            else -> null
        }
    }

    /**
     * Convertir el texto del precio a número, aceptando coma o punto decimal
     */
    private fun parsePrice(price: String): Double? =
        price.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

    /**
     * Guardar ítem
     */
    fun saveItem() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Validar formulario
                saveAttempted = true
                validateField()
                val errors = validateForm()
                if (errors.isNotEmpty()) {
                    _validationErrors.value = errors
                    _isLoading.value = false
                    return@launch
                }

                val state = _uiState.value
                val priceValue = if (state.price.isBlank()) null else parsePrice(state.price)
                android.util.Log.d("CreateEditVM", "priceValue after conversion: $priceValue")

                // Crear o actualizar ítem
                val item = if (isEditMode && currentItem != null) {
                    currentItem!!.copy(
                        type = state.itemType,
                        name = state.name.trim(),
                        price = priceValue,
                        purchaseDate = state.purchaseDate,
                        expiryDate = state.expiryDate,
                        billingFrequency = state.billingFrequency,
                        imagePath = state.imagePath,
                        isActive = state.isActive,
                        category = state.category,
                        categoryColor = state.categoryColor,
                        isFreeTrial = state.isFreeTrial,
                        freeTrialEndDate = state.freeTrialEndDate,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    Item(
                        type = state.itemType,
                        name = state.name.trim(),
                        price = priceValue,
                        purchaseDate = state.purchaseDate,
                        expiryDate = state.expiryDate,
                        billingFrequency = state.billingFrequency,
                        imagePath = state.imagePath,
                        isActive = state.isActive,
                        category = state.category,
                        categoryColor = state.categoryColor,
                        isFreeTrial = state.isFreeTrial,
                        freeTrialEndDate = state.freeTrialEndDate
                    )
                }

                // Guardar en repositorio
                val result = if (isEditMode) {
                    repository.updateItem(item)
                } else {
                    repository.insertItem(item)
                }

                result.fold(
                    onSuccess = { 
                        _saveResult.value = SaveResult.Success
                    },
                    onFailure = { exception ->
                        _saveResult.value = SaveResult.Error(exception.message ?: "Error desconocido")
                    }
                )

            } catch (e: Exception) {
                _saveResult.value = SaveResult.Error("Error al guardar: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Limpiar errores de validación
     */
    private fun clearValidationErrors() {
        _validationErrors.value = emptyList()
    }

    /**
     * Validar campos en tiempo real (muestra errores sin bloquear)
     */
    private fun validateField() {
        val state = _uiState.value
        _fieldErrors.value = FieldErrors(
            name = if (saveAttempted || FIELD_NAME in touchedFields) validateName(state.name) else null,
            price = if (saveAttempted || FIELD_PRICE in touchedFields) validatePrice(state.price) else null
        )
    }

    /**
     * Errores en línea de los campos de texto
     */
    data class FieldErrors(
        val name: String? = null,
        val price: String? = null
    )

    /**
     * Limpiar resultado de guardado
     */
    fun clearSaveResult() {
        _saveResult.value = null
    }

    /**
     * Verificar si el formulario tiene cambios
     */
    fun hasChanges(): Boolean {
        if (!isEditMode) return true // Nuevo ítem siempre tiene cambios
        
        val state = _uiState.value
        val original = currentItem ?: return true
        
        return state.name != original.name ||
                state.price != (original.price?.toString() ?: "") ||
                state.purchaseDate != original.purchaseDate ||
                state.expiryDate != original.expiryDate ||
                state.billingFrequency != original.billingFrequency ||
                state.imagePath != original.imagePath ||
                state.isActive != original.isActive ||
                state.category != original.category ||
                state.categoryColor != original.categoryColor ||
                state.isFreeTrial != original.isFreeTrial ||
                state.freeTrialEndDate != original.freeTrialEndDate
    }

    /**
     * Estado del UI para crear/editar
     */
    data class CreateEditUiState(
        val itemType: Int = Constants.ITEM_TYPE_SUBSCRIPTION,
        val name: String = "",
        val price: String = "",
        val purchaseDate: Long = System.currentTimeMillis(),
        val expiryDate: Long = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L),
        val billingFrequency: String? = Constants.FREQUENCY_MONTHLY,
        val imagePath: String? = null,
        val isActive: Boolean = true,
        val isEditMode: Boolean = false,
        val category: String? = null,
        val categoryColor: String? = null,
        val isFreeTrial: Boolean = false,
        val freeTrialEndDate: Long? = null
    )

    /**
     * Resultado de operación de guardado
     */
    sealed class SaveResult {
        object Success : SaveResult()
        data class Error(val message: String) : SaveResult()
    }

    companion object {
        const val MAX_NAME_LENGTH = 60
        const val MAX_CATEGORY_LENGTH = 30
        private const val MAX_PRICE = 9_999_999.99
        private const val FIELD_NAME = "name"
        private const val FIELD_PRICE = "price"
    }
}