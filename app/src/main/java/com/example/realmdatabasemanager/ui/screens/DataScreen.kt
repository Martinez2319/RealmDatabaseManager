package com.example.realmdatabasemanager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DataArray
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.realmdatabasemanager.viewmodels.DataViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    viewModel: DataViewModel,
    onBackClick: () -> Unit
) {
    val data by viewModel.data.collectAsState()
    val fields by viewModel.fields.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Datos",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetRealmConnection() }) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Reiniciar conexión"
                        )
                    }
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
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
                onClick = { viewModel.showCreateDataDialog() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nuevo registro") },
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
                visible = !isLoading && data.isEmpty(),
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
                        Icons.Outlined.DataArray,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay datos",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Crea un nuevo registro con el botón +",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = !isLoading && data.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(data) { item ->
                        DataItem(
                            data = item,
                            fields = fields,
                            onEdit = { viewModel.showEditDataDialog(item) },
                            onDelete = { viewModel.showDeleteDataDialog(item) }
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
            CreateDataDialog(
                fields = fields,
                onDismiss = { viewModel.hideCreateDataDialog() },
                onConfirm = { values -> viewModel.createData(values) }
            )
        }

        val selectedData = viewModel.selectedData.collectAsState().value
        if (viewModel.showEditDialog.collectAsState().value && selectedData != null) {
            EditDataDialog(
                data = selectedData,
                fields = fields,
                onDismiss = { viewModel.hideEditDataDialog() },
                onConfirm = { values ->
                    // Pasar el objeto completo para actualizar por filtro
                    viewModel.updateData(selectedData, values)
                }
            )
        }

        if (viewModel.showDeleteDialog.collectAsState().value && selectedData != null) {
            DeleteDataDialog(
                onDismiss = { viewModel.hideDeleteDataDialog() },
                onConfirm = {
                    // Pasar el objeto completo para eliminar
                    viewModel.deleteData(selectedData)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataItem(
    data: Map<String, Any>,
    fields: List<Pair<String, String>>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Mostrar campos
            fields.forEach { (fieldName, fieldType) ->
                // Excluir campos "id" y campos internos
                if (fieldName.lowercase() != "id" && !fieldName.startsWith("__") && data.containsKey(fieldName)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$fieldName:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.3f)
                        )
                        Text(
                            text = data[fieldName].toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(0.7f)
                        )
                    }
                }
            }

            // Botones de acción
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.padding(end = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar")
                }
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Eliminar")
                }
            }
        }
    }
}

@Composable
fun CreateDataDialog(
    fields: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    val values = remember { mutableStateMapOf<String, Any>() }
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
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Crear Registro",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                fields.forEach { (fieldName, fieldType) ->
                    if (fieldName != "id") { // No permitir editar el ID
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "$fieldName ($fieldType)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        when (fieldType) {
                            "STRING" -> {
                                OutlinedTextField(
                                    value = values[fieldName]?.toString() ?: "",
                                    onValueChange = { values[fieldName] = it },
                                    label = { Text(fieldName) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    shape = MaterialTheme.shapes.medium
                                )
                            }
                            "INTEGER" -> {
                                OutlinedTextField(
                                    value = values[fieldName]?.toString() ?: "",
                                    onValueChange = {
                                        try {
                                            if (it.isNotEmpty()) {
                                                values[fieldName] = it.toInt()
                                                error = ""
                                            } else {
                                                values.remove(fieldName)
                                            }
                                        } catch (e: NumberFormatException) {
                                            error = "Valor inválido para $fieldName. Debe ser un número entero."
                                        }
                                    },
                                    label = { Text(fieldName) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    isError = error.contains(fieldName),
                                    shape = MaterialTheme.shapes.medium
                                )
                            }
                            "DOUBLE" -> {
                                OutlinedTextField(
                                    value = values[fieldName]?.toString() ?: "",
                                    onValueChange = {
                                        try {
                                            if (it.isNotEmpty()) {
                                                values[fieldName] = it.toDouble()
                                                error = ""
                                            } else {
                                                values.remove(fieldName)
                                            }
                                        } catch (e: NumberFormatException) {
                                            error = "Valor inválido para $fieldName. Debe ser un número decimal."
                                        }
                                    },
                                    label = { Text(fieldName) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    isError = error.contains(fieldName),
                                    shape = MaterialTheme.shapes.medium
                                )
                            }
                            "BOOLEAN" -> {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = fieldName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Switch(
                                            checked = values[fieldName] as? Boolean ?: false,
                                            onCheckedChange = { values[fieldName] = it }
                                        )
                                    }
                                }
                            }
                            "JSON" -> {
                                OutlinedTextField(
                                    value = values[fieldName]?.toString() ?: "",
                                    onValueChange = { values[fieldName] = it },
                                    label = { Text(fieldName) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .height(120.dp),
                                    minLines = 3,
                                    shape = MaterialTheme.shapes.medium
                                )
                            }
                        }
                    }
                }

                if (error.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
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
                            if (values.isEmpty()) {
                                error = "Debes completar al menos un campo"
                            } else if (error.isEmpty()) {
                                onConfirm(values)
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
fun EditDataDialog(
    data: Map<String, Any>,
    fields: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    val values = remember { mutableStateMapOf<String, Any>().apply { putAll(data) } }
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
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Editar Registro",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Mostrar ID (no editable)
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ID:",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "${data["id"]}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                fields.forEach { (fieldName, fieldType) ->
                    if (fieldName != "id") { // No permitir editar el ID
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "$fieldName ($fieldType)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        when (fieldType) {
                            "STRING" -> {
                                OutlinedTextField(
                                    value = values[fieldName]?.toString() ?: "",
                                    onValueChange = { values[fieldName] = it },
                                    label = { Text(fieldName) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    shape = MaterialTheme.shapes.medium
                                )
                            }
                            "INTEGER" -> {
                                OutlinedTextField(
                                    value = values[fieldName]?.toString() ?: "",
                                    onValueChange = {
                                        try {
                                            if (it.isNotEmpty()) {
                                                values[fieldName] = it.toInt()
                                                error = ""
                                            } else {
                                                values.remove(fieldName)
                                            }
                                        } catch (e: NumberFormatException) {
                                            error = "Valor inválido para $fieldName. Debe ser un número entero."
                                        }
                                    },
                                    label = { Text(fieldName) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    isError = error.contains(fieldName),
                                    shape = MaterialTheme.shapes.medium
                                )
                            }
                            "DOUBLE" -> {
                                OutlinedTextField(
                                    value = values[fieldName]?.toString() ?: "",
                                    onValueChange = {
                                        try {
                                            if (it.isNotEmpty()) {
                                                values[fieldName] = it.toDouble()
                                                error = ""
                                            } else {
                                                values.remove(fieldName)
                                            }
                                        } catch (e: NumberFormatException) {
                                            error = "Valor inválido para $fieldName. Debe ser un número decimal."
                                        }
                                    },
                                    label = { Text(fieldName) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    isError = error.contains(fieldName),
                                    shape = MaterialTheme.shapes.medium
                                )
                            }
                            "BOOLEAN" -> {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = fieldName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Switch(
                                            checked = values[fieldName] as? Boolean ?: false,
                                            onCheckedChange = { values[fieldName] = it }
                                        )
                                    }
                                }
                            }
                            "JSON" -> {
                                OutlinedTextField(
                                    value = values[fieldName]?.toString() ?: "",
                                    onValueChange = { values[fieldName] = it },
                                    label = { Text(fieldName) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .height(120.dp),
                                    minLines = 3,
                                    shape = MaterialTheme.shapes.medium
                                )
                            }
                        }
                    }
                }

                if (error.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
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
                            if (values.isEmpty()) {
                                error = "Debes completar al menos un campo"
                            } else if (error.isEmpty()) {
                                onConfirm(values)
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
fun DeleteDataDialog(
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
                    text = "Eliminar Registro",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "¿Estás seguro de que deseas eliminar este registro?",
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