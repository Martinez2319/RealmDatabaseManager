package com.example.realmdatabasemanager.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.realmdatabasemanager.data.FieldType
import com.example.realmdatabasemanager.database.RealmManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FieldViewModel(
    private val realmManager: RealmManager,
    private val databaseName: String,
    private val collectionName: String
) : ViewModel() {

    // Cambiado para coincidir con el tipo que devuelve RealmManager
    private val _fields = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val fields: StateFlow<List<Pair<String, String>>> = _fields.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentCollection = MutableStateFlow(collectionName)
    val currentCollection: StateFlow<String> = _currentCollection.asStateFlow()

    val showCreateDialog = mutableStateOf(false)
    val showEditDialog = mutableStateOf(false)
    val showDeleteDialog = mutableStateOf(false)

    val selectedField = mutableStateOf<String?>(null)
    val selectedFieldType = mutableStateOf<FieldType?>(null)

    init {
        refreshFields()
    }

    fun refreshFields() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _fields.value = realmManager.listFields(databaseName, collectionName)
            } catch (e: Exception) {
                // Manejar error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createField(name: String, type: FieldType) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Convertir FieldType a String para RealmManager
                realmManager.createField(databaseName, collectionName, name, type.name)
                refreshFields()
            } catch (e: Exception) {
                // Manejar error
            } finally {
                _isLoading.value = false
                hideCreateFieldDialog()
            }
        }
    }

    fun updateField(newName: String, newType: FieldType) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                selectedField.value?.let { oldName ->
                    // Convertir FieldType a String para RealmManager
                    realmManager.updateField(databaseName, collectionName, oldName, newName, newType.name)
                    refreshFields()
                }
            } catch (e: Exception) {
                // Manejar error
            } finally {
                _isLoading.value = false
                hideEditFieldDialog()
            }
        }
    }

    fun deleteField() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                selectedField.value?.let { name ->
                    realmManager.deleteField(databaseName, collectionName, name)
                    refreshFields()
                }
            } catch (e: Exception) {
                // Manejar error
            } finally {
                _isLoading.value = false
                hideDeleteFieldDialog()
            }
        }
    }

    fun showCreateFieldDialog() {
        showCreateDialog.value = true
    }

    fun hideCreateFieldDialog() {
        showCreateDialog.value = false
    }

    fun showEditFieldDialog(fieldName: String, fieldTypeStr: String) {
        selectedField.value = fieldName
        // Convertir String a FieldType
        selectedFieldType.value = try {
            FieldType.valueOf(fieldTypeStr)
        } catch (e: Exception) {
            FieldType.STRING // Valor por defecto
        }
        showEditDialog.value = true
    }

    fun hideEditFieldDialog() {
        showEditDialog.value = false
    }

    fun showDeleteFieldDialog(fieldName: String) {
        selectedField.value = fieldName
        showDeleteDialog.value = true
    }

    fun hideDeleteFieldDialog() {
        showDeleteDialog.value = false
    }
}