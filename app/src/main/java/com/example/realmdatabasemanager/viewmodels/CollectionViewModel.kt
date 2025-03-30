package com.example.realmdatabasemanager.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.realmdatabasemanager.database.RealmManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CollectionViewModel(
    private val realmManager: RealmManager,
    private val databaseName: String
) : ViewModel() {

    private val _collections = MutableStateFlow<List<String>>(emptyList())
    val collections: StateFlow<List<String>> = _collections.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentDatabase = MutableStateFlow(databaseName)
    val currentDatabase: StateFlow<String> = _currentDatabase.asStateFlow()

    val showCreateDialog = mutableStateOf(false)
    val showRenameDialog = mutableStateOf(false)
    val showDeleteDialog = mutableStateOf(false)

    val selectedCollection = mutableStateOf<String?>(null)

    init {
        refreshCollections()
    }

    fun refreshCollections() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _collections.value = realmManager.listCollections(databaseName)
            } catch (e: Exception) {
                // Manejar error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createCollection(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                realmManager.createCollection(databaseName, name)
                refreshCollections()
            } catch (e: Exception) {
                // Manejar error
            } finally {
                _isLoading.value = false
                hideCreateCollectionDialog()
            }
        }
    }

    fun renameCollection(newName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                selectedCollection.value?.let { oldName ->
                    realmManager.renameCollection(databaseName, oldName, newName)
                    refreshCollections()
                }
            } catch (e: Exception) {
                // Manejar error
            } finally {
                _isLoading.value = false
                hideRenameCollectionDialog()
            }
        }
    }

    fun deleteCollection() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                selectedCollection.value?.let { name ->
                    realmManager.deleteCollection(databaseName, name)
                    refreshCollections()
                }
            } catch (e: Exception) {
                // Manejar error
            } finally {
                _isLoading.value = false
                hideDeleteCollectionDialog()
            }
        }
    }

    fun showCreateCollectionDialog() {
        showCreateDialog.value = true
    }

    fun hideCreateCollectionDialog() {
        showCreateDialog.value = false
    }

    fun showRenameCollectionDialog(collectionName: String) {
        selectedCollection.value = collectionName
        showRenameDialog.value = true
    }

    fun hideRenameCollectionDialog() {
        showRenameDialog.value = false
    }

    fun showDeleteCollectionDialog(collectionName: String) {
        selectedCollection.value = collectionName
        showDeleteDialog.value = true
    }

    fun hideDeleteCollectionDialog() {
        showDeleteDialog.value = false
    }
}