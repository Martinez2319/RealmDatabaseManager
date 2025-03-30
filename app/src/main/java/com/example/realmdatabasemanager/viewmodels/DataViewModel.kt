package com.example.realmdatabasemanager.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.realmdatabasemanager.database.RealmManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class DataViewModel(
    private val realmManager: RealmManager,
    private val databaseName: String,
    private val collectionName: String
) : ViewModel() {

    private val _data = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val data: StateFlow<List<Map<String, Any>>> = _data.asStateFlow()

    private val _fields = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val fields: StateFlow<List<Pair<String, String>>> = _fields.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    private val _selectedData = MutableStateFlow<Map<String, Any>?>(null)
    val selectedData: StateFlow<Map<String, Any>?> = _selectedData.asStateFlow()

    init {
        refreshData()
        loadFields()
    }

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val dataList = realmManager.queryData(databaseName, collectionName)
                _data.value = dataList as List<Map<String, Any>>
            } catch (e: Exception) {
                _error.value = "Error al cargar datos: ${e.message}"
                Log.e("DataViewModel", "Error al cargar datos", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadFields() {
        viewModelScope.launch {
            try {
                val fieldList = realmManager.listFields(databaseName, collectionName)
                _fields.value = fieldList
            } catch (e: Exception) {
                _error.value = "Error al cargar campos: ${e.message}"
                Log.e("DataViewModel", "Error al cargar campos", e)
            }
        }
    }

    fun createData(values: Map<String, Any>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val success = realmManager.insertData(databaseName, collectionName, values)
                if (success) {
                    refreshData()
                } else {
                    _error.value = "No se pudo crear el registro"
                }
            } catch (e: Exception) {
                _error.value = "Error al crear registro: ${e.message}"
                Log.e("DataViewModel", "Error al crear registro", e)
            } finally {
                _isLoading.value = false
                _showCreateDialog.value = false
            }
        }
    }

    // Método actualizado para actualizar datos por filtro en lugar de por ID
    fun updateData(data: Map<String, Any>, newValues: Map<String, Any>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val position = data["__position"] as? Int

                val success = if (position != null) {
                    // Actualizar por posición
                    realmManager.updateData(databaseName, collectionName, position = position, values = newValues)
                } else {
                    // Actualizar por filtro (todos los campos visibles)
                    val filter = data.filterKeys { !it.startsWith("__") }
                    if (filter.isEmpty()) {
                        false
                    } else {
                        realmManager.updateData(databaseName, collectionName, filter = filter, values = newValues)
                    }
                }

                if (success) {
                    refreshData()
                } else {
                    _error.value = "No se pudo actualizar el registro"
                }
            } catch (e: Exception) {
                _error.value = "Error al actualizar registro: ${e.message}"
                Log.e("DataViewModel", "Error al actualizar registro", e)
            } finally {
                _isLoading.value = false
                _showEditDialog.value = false
                _selectedData.value = null
            }
        }
    }

    // Método mejorado para eliminar datos por posición o filtro
    fun deleteData(data: Map<String, Any>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val position = data["__position"] as? Int

                Log.d("DataViewModel", "Intentando eliminar registro: $data")

                val success = if (position != null) {
                    // Eliminar por posición
                    Log.d("DataViewModel", "Eliminando por posición: $position")
                    realmManager.deleteData(databaseName, collectionName, position = position)
                } else {
                    // Eliminar por filtro (todos los campos visibles)
                    val filter = data.filterKeys { !it.startsWith("__") }
                    if (filter.isEmpty()) {
                        Log.e("DataViewModel", "No hay filtros para eliminar")
                        false
                    } else {
                        Log.d("DataViewModel", "Eliminando por filtro: $filter")
                        realmManager.deleteData(databaseName, collectionName, filter = filter)
                    }
                }

                if (success) {
                    Log.d("DataViewModel", "Registro eliminado correctamente")
                    refreshData()
                } else {
                    Log.e("DataViewModel", "No se pudo eliminar el registro")
                    _error.value = "No se pudo eliminar el registro. Intenta reiniciar la conexión."
                }
            } catch (e: Exception) {
                _error.value = "Error al eliminar registro: ${e.message}"
                Log.e("DataViewModel", "Error al eliminar registro", e)
            } finally {
                _isLoading.value = false
                _showDeleteDialog.value = false
                _selectedData.value = null
            }
        }
    }

    fun showCreateDataDialog() {
        _showCreateDialog.value = true
    }

    fun hideCreateDataDialog() {
        _showCreateDialog.value = false
    }

    fun showEditDataDialog(data: Map<String, Any>) {
        _selectedData.value = data
        _showEditDialog.value = true
    }

    fun hideEditDataDialog() {
        _showEditDialog.value = false
        _selectedData.value = null
    }

    fun showDeleteDataDialog(data: Map<String, Any>) {
        _selectedData.value = data
        _showDeleteDialog.value = true
    }

    fun hideDeleteDataDialog() {
        _showDeleteDialog.value = false
        _selectedData.value = null
    }

    fun clearError() {
        _error.value = null
    }

    // Método para reiniciar la conexión a Realm
    fun resetRealmConnection() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val success = realmManager.resetConnection()
                if (success) {
                    Log.d("DataViewModel", "Conexión a Realm reiniciada correctamente")
                    // Refrescar los datos
                    refreshData()
                } else {
                    _error.value = "No se pudo reiniciar la conexión a Realm"
                    Log.e("DataViewModel", "No se pudo reiniciar la conexión a Realm")
                }
            } catch (e: Exception) {
                _error.value = "Error al reiniciar conexión a Realm: ${e.message}"
                Log.e("DataViewModel", "Error al reiniciar conexión a Realm", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class DataViewModelFactory(
    private val realmManager: RealmManager,
    private val databaseName: String,
    private val collectionName: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DataViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DataViewModel(realmManager, databaseName, collectionName) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}