package com.example.realmdatabasemanager.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.realmdatabasemanager.database.RealmManager
import com.example.realmdatabasemanager.viewmodels.DatabaseViewModel
import com.example.realmdatabasemanager.viewmodels.CollectionViewModel
import com.example.realmdatabasemanager.viewmodels.FieldViewModel
import com.example.realmdatabasemanager.viewmodels.DataViewModel

class ViewModelFactory(
    private val realmManager: RealmManager,
    private val databaseName: String = "",
    private val collectionName: String = ""
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DatabaseViewModel::class.java) -> {
                DatabaseViewModel(realmManager) as T
            }
            modelClass.isAssignableFrom(CollectionViewModel::class.java) -> {
                CollectionViewModel(realmManager, databaseName) as T
            }
            modelClass.isAssignableFrom(FieldViewModel::class.java) -> {
                FieldViewModel(realmManager, databaseName, collectionName) as T
            }
            modelClass.isAssignableFrom(DataViewModel::class.java) -> {
                DataViewModel(realmManager, databaseName, collectionName) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}