package com.example.realmdatabasemanager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.realmdatabasemanager.viewmodels.DatabaseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseListScreen(
    viewModel: DatabaseViewModel,
    onDatabaseSelected: (String) -> Unit
) {
    val databases by viewModel.databases.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Bases de Datos",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.resetRealmConnection() }) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reiniciar conexión"
                        )
                    }
                    IconButton(onClick = { viewModel.refreshDatabases() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refrescar"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showCreateDatabaseDialog() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nueva base de datos") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            AnimatedVisibility(
                visible = !isLoading && databases.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Outlined.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay bases de datos",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Crea una nueva con el botón +",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = !isLoading && databases.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(databases) { database ->
                        DatabaseItem(
                            databaseName = database,
                            onSelect = { onDatabaseSelected(database) },
                            onRename = { viewModel.showRenameDatabaseDialog(database) },
                            onDelete = { viewModel.showDeleteDatabaseDialog(database) }
                        )
                    }
                }
            }

            // Mostrar error si existe
            error?.let { errorMessage ->
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK", color = MaterialTheme.colorScheme.inversePrimary)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Text(errorMessage)
                }
            }
        }

        // Diálogos
        if (viewModel.showCreateDialog.collectAsState().value) {
            CreateDatabaseDialog(
                onDismiss = { viewModel.hideCreateDatabaseDialog() },
                onConfirm = { name -> viewModel.createDatabase(name) }
            )
        }

        val databaseToRename = viewModel.databaseToRename.collectAsState().value
        if (viewModel.showRenameDialog.collectAsState().value && databaseToRename != null) {
            RenameDatabaseDialog(
                currentName = databaseToRename,
                onDismiss = { viewModel.hideRenameDatabaseDialog() },
                onConfirm = { newName -> viewModel.renameDatabase(databaseToRename, newName) }
            )
        }

        val databaseToDelete = viewModel.databaseToDelete.collectAsState().value
        if (viewModel.showDeleteDialog.collectAsState().value && databaseToDelete != null) {
            DeleteDatabaseDialog(
                databaseName = databaseToDelete,
                onDismiss = { viewModel.hideDeleteDatabaseDialog() },
                onConfirm = { viewModel.deleteDatabase(databaseToDelete) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseItem(
    databaseName: String,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = MaterialTheme.shapes.medium),
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Storage,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = databaseName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row {
                IconButton(onClick = onRename) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Renombrar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun CreateDatabaseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var databaseName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Crear Base de Datos",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = databaseName,
                    onValueChange = {
                        databaseName = it
                        error = ""
                    },
                    label = { Text("Nombre de la Base de Datos") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = error.isNotEmpty(),
                    supportingText = {
                        if (error.isNotEmpty()) {
                            Text(text = error, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (databaseName.isBlank()) {
                                error = "El nombre no puede estar vacío"
                            } else {
                                onConfirm(databaseName)
                            }
                        },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Crear")
                    }
                }
            }
        }
    }
}

@Composable
fun RenameDatabaseDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    var error by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Renombrar Base de Datos",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        error = ""
                    },
                    label = { Text("Nuevo nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = error.isNotEmpty(),
                    supportingText = {
                        if (error.isNotEmpty()) {
                            Text(text = error, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newName.isBlank()) {
                                error = "El nombre no puede estar vacío"
                            } else {
                                onConfirm(newName)
                            }
                        },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Renombrar")
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteDatabaseDialog(
    databaseName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Eliminar Base de Datos",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "¿Estás seguro de que deseas eliminar la base de datos '$databaseName'?",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Eliminar")
                    }
                }
            }
        }
    }
}