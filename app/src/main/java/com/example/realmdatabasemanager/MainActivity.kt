package com.example.realmdatabasemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.realmdatabasemanager.database.RealmManager
import com.example.realmdatabasemanager.ui.screens.CollectionListScreen
import com.example.realmdatabasemanager.ui.screens.DataScreen
import com.example.realmdatabasemanager.ui.screens.DatabaseListScreen
import com.example.realmdatabasemanager.ui.screens.FieldListScreen
import com.example.realmdatabasemanager.ui.theme.RealmDatabaseManagerTheme
import com.example.realmdatabasemanager.viewmodels.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar RealmManager
        val realmManager = RealmManager(applicationContext)

        setContent {
            RealmDatabaseManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(realmManager)
                }
            }
        }
    }
}

// Factory para ViewModels
class ViewModelFactory(
    private val realmManager: RealmManager,
    private val databaseName: String? = null,
    private val collectionName: String? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DatabaseViewModel::class.java) -> {
                DatabaseViewModel(realmManager) as T
            }
            modelClass.isAssignableFrom(CollectionViewModel::class.java) -> {
                if (databaseName == null) {
                    throw IllegalArgumentException("Database name is required for CollectionViewModel")
                }
                CollectionViewModel(realmManager, databaseName) as T
            }
            modelClass.isAssignableFrom(FieldViewModel::class.java) -> {
                if (databaseName == null || collectionName == null) {
                    throw IllegalArgumentException("Database and collection names are required for FieldViewModel")
                }
                FieldViewModel(realmManager, databaseName, collectionName) as T
            }
            modelClass.isAssignableFrom(DataViewModel::class.java) -> {
                if (databaseName == null || collectionName == null) {
                    throw IllegalArgumentException("Database and collection names are required for DataViewModel")
                }
                DataViewModel(realmManager, databaseName, collectionName) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

@Composable
fun AppNavigation(realmManager: RealmManager) {
    var currentScreen by remember { mutableStateOf("databases") }
    var currentDatabase by remember { mutableStateOf<String?>(null) }
    var currentCollection by remember { mutableStateOf<String?>(null) }

    when (currentScreen) {
        "databases" -> {
            val databaseViewModel = viewModel<DatabaseViewModel>(
                factory = ViewModelFactory(realmManager)
            )

            DatabaseListScreen(
                viewModel = databaseViewModel,
                onDatabaseSelected = { database ->
                    currentDatabase = database
                    currentScreen = "collections"
                }
            )
        }
        "collections" -> {
            if (currentDatabase == null) {
                currentScreen = "databases"
                return
            }

            val collectionViewModel = viewModel<CollectionViewModel>(
                key = "collection_${currentDatabase}",
                factory = ViewModelFactory(realmManager, currentDatabase)
            )

            CollectionListScreen(
                viewModel = collectionViewModel,
                onNavigateBack = {
                    currentScreen = "databases"
                    currentDatabase = null
                },
                onCollectionSelected = { collection ->
                    currentCollection = collection
                    currentScreen = "fields"
                }
            )
        }
        "fields" -> {
            if (currentDatabase == null || currentCollection == null) {
                currentScreen = "collections"
                currentCollection = null
                return
            }

            val fieldViewModel = viewModel<FieldViewModel>(
                key = "field_${currentDatabase}_${currentCollection}",
                factory = ViewModelFactory(
                    realmManager,
                    currentDatabase,
                    currentCollection
                )
            )

            FieldListScreen(
                viewModel = fieldViewModel,
                onNavigateBack = {
                    currentScreen = "collections"
                    currentCollection = null
                },
                onViewData = {
                    currentScreen = "data"
                }
            )
        }
        "data" -> {
            if (currentDatabase == null || currentCollection == null) {
                currentScreen = "fields"
                return
            }

            val dataViewModel = viewModel<DataViewModel>(
                key = "data_${currentDatabase}_${currentCollection}",
                factory = ViewModelFactory(
                    realmManager,
                    currentDatabase,
                    currentCollection
                )
            )

            DataScreen(
                viewModel = dataViewModel,
                onBackClick = {
                    currentScreen = "fields"
                }
            )
        }
    }
}