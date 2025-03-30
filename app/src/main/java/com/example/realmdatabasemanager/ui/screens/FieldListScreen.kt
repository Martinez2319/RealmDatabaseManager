package com.example.realmdatabasemanager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.realmdatabasemanager.data.FieldType
import com.example.realmdatabasemanager.viewmodels.FieldViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldListScreen(
    viewModel: FieldViewModel,
    onNavigateBack: () -> Unit,
    onViewData: () -> Unit
) {
    val fields by viewModel.fields.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentCollection by viewModel.currentCollection.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Campos de ${currentCollection ?: ""}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshFields() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                    }
                    FilledTonalIconButton(
                        onClick = onViewData,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.DataArray, contentDescription = "Ver Datos")
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
                onClick = { viewModel.showCreateFieldDialog() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nuevo campo") },
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
                visible = !isLoading && fields.isEmpty(),
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
                        Icons.Outlined.Code,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay campos",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Crea uno nuevo con el botón +",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = !isLoading && fields.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(fields) { field ->
                        FieldItem(
                            fieldName = field.first,
                            fieldTypeStr = field.second,
                            onEdit = {
                                viewModel.showEditFieldDialog(field.first, field.second)
                            },
                            onDelete = {
                                viewModel.showDeleteFieldDialog(field.first)
                            }
                        )
                    }
                }
            }
        }

        // Diálogos
        if (viewModel.showCreateDialog.value) {
            CreateFieldDialog(
                onDismiss = { viewModel.hideCreateFieldDialog() },
                onConfirm = { name, type -> viewModel.createField(name, type) }
            )
        }

        if (viewModel.showEditDialog.value) {
            EditFieldDialog(
                currentName = viewModel.selectedField.value ?: "",
                currentType = viewModel.selectedFieldType.value ?: FieldType.STRING,
                onDismiss = { viewModel.hideEditFieldDialog() },
                onConfirm = { newName, newType -> viewModel.updateField(newName, newType) }
            )
        }

        if (viewModel.showDeleteDialog.value) {
            DeleteFieldDialog(
                fieldName = viewModel.selectedField.value ?: "",
                onDismiss = { viewModel.hideDeleteFieldDialog() },
                onConfirm = { viewModel.deleteField() }
            )
        }
    }
}

@Composable
fun FieldItem(
    fieldName: String,
    fieldTypeStr: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // Convertir el string del tipo a FieldType para mostrar el displayName
    val fieldType = try {
        FieldType.valueOf(fieldTypeStr)
    } catch (e: Exception) {
        null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = MaterialTheme.shapes.medium),
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
            Column {
                Text(
                    text = fieldName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        // Si podemos convertir a FieldType, usamos displayName, si no, mostramos el string directamente
                        text = fieldType?.displayName ?: fieldTypeStr,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
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
fun CreateFieldDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, FieldType) -> Unit
) {
    var fieldName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(FieldType.STRING) }
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
                    text = "Crear Campo",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = fieldName,
                    onValueChange = {
                        fieldName = it
                        error = ""
                    },
                    label = { Text("Nombre del Campo") },
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

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Tipo de Dato",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        FieldType.values().forEach { type ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedType == type,
                                    onClick = { selectedType = type },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    text = type.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

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
                            if (fieldName.isBlank()) {
                                error = "El nombre no puede estar vacío"
                            } else {
                                onConfirm(fieldName, selectedType)
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
fun EditFieldDialog(
    currentName: String,
    currentType: FieldType,
    onDismiss: () -> Unit,
    onConfirm: (String, FieldType) -> Unit
) {
    var fieldName by remember { mutableStateOf(currentName) }
    var selectedType by remember { mutableStateOf(currentType) }
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
                    text = "Editar Campo",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = fieldName,
                    onValueChange = {
                        fieldName = it
                        error = ""
                    },
                    label = { Text("Nombre del Campo") },
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

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Tipo de Dato",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        FieldType.values().forEach { type ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedType == type,
                                    onClick = { selectedType = type },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    text = type.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

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
                            if (fieldName.isBlank()) {
                                error = "El nombre no puede estar vacío"
                            } else {
                                onConfirm(fieldName, selectedType)
                            }
                        },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteFieldDialog(
    fieldName: String,
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
                    text = "Eliminar Campo",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "¿Estás seguro de que deseas eliminar el campo '$fieldName'?",
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