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

class DatabaseViewModel(private val realmManager: RealmManager) : ViewModel() {

    private val _databases = MutableStateFlow<List<String>>(emptyList())
    val databases: StateFlow<List<String>> = _databases.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentDatabase = MutableStateFlow<String?>(null)
    val currentDatabase: StateFlow<String?> = _currentDatabase.asStateFlow()

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _showRenameDialog = MutableStateFlow(false)
    val showRenameDialog: StateFlow<Boolean> = _showRenameDialog.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    private val _databaseToRename = MutableStateFlow<String?>(null)
    val databaseToRename: StateFlow<String?> = _databaseToRename.asStateFlow()

    private val _databaseToDelete = MutableStateFlow<String?>(null)
    val databaseToDelete: StateFlow<String?> = _databaseToDelete.asStateFlow()

    init {
        refreshDatabases()
    }

    fun refreshDatabases() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val databaseList = realmManager.listDatabases()
                _databases.value = databaseList
            } catch (e: Exception) {
                _error.value = "Error al cargar bases de datos: ${e.message}"
                Log.e("DatabaseViewModel", "Error al cargar bases de datos", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createDatabase(name: String) {
        if (name.isBlank()) {
            _error.value = "El nombre de la base de datos no puede estar vacío"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val success = realmManager.createDatabase(name)
                if (success) {
                    refreshDatabases()
                } else {
                    _error.value = "No se pudo crear la base de datos"
                }
            } catch (e: Exception) {
                _error.value = "Error al crear base de datos: ${e.message}"
                Log.e("DatabaseViewModel", "Error al crear base de datos", e)
            } finally {
                _isLoading.value = false
                _showCreateDialog.value = false
            }
        }
    }

    fun openDatabase(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val success = realmManager.openDatabase(name)
                if (success) {
                    _currentDatabase.value = name
                } else {
                    _error.value = "No se pudo abrir la base de datos"
                }
            } catch (e: Exception) {
                _error.value = "Error al abrir base de datos: ${e.message}"
                Log.e("DatabaseViewModel", "Error al abrir base de datos", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun renameDatabase(oldName: String, newName: String) {
        if (newName.isBlank()) {
            _error.value = "El nuevo nombre no puede estar vacío"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val success = realmManager.renameDatabase(oldName, newName)
                if (success) {
                    if (_currentDatabase.value == oldName) {
                        _currentDatabase.value = newName
                    }
                    refreshDatabases()
                } else {
                    _error.value = "No se pudo renombrar la base de datos"
                }
            } catch (e: Exception) {
                _error.value = "Error al renombrar base de datos: ${e.message}"
                Log.e("DatabaseViewModel", "Error al renombrar base de datos", e)
            } finally {
                _isLoading.value = false
                _showRenameDialog.value = false
                _databaseToRename.value = null
            }
        }
    }

    fun deleteDatabase(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val success = realmManager.deleteDatabase(name)
                if (success) {
                    if (_currentDatabase.value == name) {
                        _currentDatabase.value = null
                    }
                    refreshDatabases()
                } else {
                    _error.value = "No se pudo eliminar la base de datos"
                }
            } catch (e: Exception) {
                _error.value = "Error al eliminar base de datos: ${e.message}"
                Log.e("DatabaseViewModel", "Error al eliminar base de datos", e)
            } finally {
                _isLoading.value = false
                _showDeleteDialog.value = false
                _databaseToDelete.value = null
            }
        }
    }

    fun showCreateDatabaseDialog() {
        _showCreateDialog.value = true
    }

    fun hideCreateDatabaseDialog() {
        _showCreateDialog.value = false
    }

    fun showRenameDatabaseDialog(databaseName: String) {
        _databaseToRename.value = databaseName
        _showRenameDialog.value = true
    }

    fun hideRenameDatabaseDialog() {
        _showRenameDialog.value = false
        _databaseToRename.value = null
    }

    fun showDeleteDatabaseDialog(databaseName: String) {
        _databaseToDelete.value = databaseName
        _showDeleteDialog.value = true
    }

    fun hideDeleteDatabaseDialog() {
        _showDeleteDialog.value = false
        _databaseToDelete.value = null
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
                    Log.d("DatabaseViewModel", "Conexión a Realm reiniciada correctamente")
                    // Refrescar la lista de bases de datos
                    refreshDatabases()
                } else {
                    _error.value = "No se pudo reiniciar la conexión a Realm"
                    Log.e("DatabaseViewModel", "No se pudo reiniciar la conexión a Realm")
                }
            } catch (e: Exception) {
                _error.value = "Error al reiniciar conexión a Realm: ${e.message}"
                Log.e("DatabaseViewModel", "Error al reiniciar conexión a Realm", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        realmManager.closeDatabase()
    }
}

class DatabaseViewModelFactory(private val realmManager: RealmManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DatabaseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DatabaseViewModel(realmManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}