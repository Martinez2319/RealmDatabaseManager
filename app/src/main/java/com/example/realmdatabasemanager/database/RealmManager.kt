package com.example.realmdatabasemanager.database

import android.content.Context
import android.util.Log
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.Sort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.mongodb.kbson.ObjectId
import com.example.realmdatabasemanager.models.*
import java.io.File

class RealmManager(private val context: Context) {
    private var realm: Realm? = null
    private var currentDatabaseName: String? = null
    private val TAG = "RealmManager"

    // Método para obtener una instancia de Realm configurada para metadatos
    private fun getMetadataRealm(): Realm {
        val config = RealmConfiguration.Builder(
            schema = setOf(
                DatabaseMetadata::class,
                CollectionMetadata::class,
                FieldMetadata::class,
                DynamicData::class
            )
        )
            .name("metadata.realm")
            .schemaVersion(1)
            .build()

        return Realm.open(config)
    }

    // 1. Crear base de datos
    suspend fun createDatabase(name: String): Boolean = withContext(Dispatchers.IO) {
        val metadataRealm = getMetadataRealm()
        try {
            var success = false
            metadataRealm.writeBlocking {
                val existingDb =
                    metadataRealm.query<DatabaseMetadata>("name == $0", name).first().find()
                if (existingDb == null) {
                    val newDb = DatabaseMetadata().apply {
                        this.name = name
                    }
                    copyToRealm(newDb)
                    success = true
                    Log.d(TAG, "Base de datos creada: $name")
                } else {
                    Log.d(TAG, "La base de datos ya existe: $name")
                }
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear base de datos: ${e.message}", e)
            false
        } finally {
            metadataRealm.close()
        }
    }

    // 2. Listar bases de datos creadas
    suspend fun listDatabases(): List<String> = withContext(Dispatchers.IO) {
        val metadataRealm = getMetadataRealm()
        try {
            val databases = metadataRealm.query<DatabaseMetadata>()
                .sort("name", Sort.ASCENDING)
                .find()
                .map { it.name }

            Log.d(TAG, "Bases de datos encontradas: ${databases.size}")
            databases
        } catch (e: Exception) {
            Log.e(TAG, "Error al listar bases de datos: ${e.message}", e)
            emptyList()
        } finally {
            metadataRealm.close()
        }
    }

    // 3. Iniciar base de datos
    suspend fun openDatabase(name: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Cerrar la base de datos actual si está abierta
            realm?.close()

            val config = RealmConfiguration.Builder(
                schema = setOf(
                    DatabaseMetadata::class,
                    CollectionMetadata::class,
                    FieldMetadata::class,
                    DynamicData::class
                )
            )
                .name("$name.realm")
                .schemaVersion(1)
                .build()

            realm = Realm.open(config)
            currentDatabaseName = name
            Log.d(TAG, "Base de datos abierta: $name")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al abrir base de datos: ${e.message}", e)
            false
        }
    }

    // 4. Modificar o alterar una base de datos
    suspend fun renameDatabase(oldName: String, newName: String): Boolean =
        withContext(Dispatchers.IO) {
            val metadataRealm = getMetadataRealm()
            try {
                var success = false

                // Verificar si la nueva base de datos ya existe
                val newDbExists =
                    metadataRealm.query<DatabaseMetadata>("name == $0", newName).first()
                        .find() != null
                if (newDbExists) {
                    Log.d(TAG, "No se puede renombrar, el nuevo nombre ya existe: $newName")
                    return@withContext false
                }

                metadataRealm.writeBlocking {
                    val db =
                        metadataRealm.query<DatabaseMetadata>("name == $0", oldName).first().find()
                    if (db != null) {
                        val latestDb = findLatest(db)
                        if (latestDb != null) {
                            latestDb.name = newName
                            latestDb.lastModified = System.currentTimeMillis()
                            success = true
                            Log.d(TAG, "Base de datos renombrada: $oldName -> $newName")
                        } else {
                            Log.d(
                                TAG,
                                "No se pudo encontrar la versión más reciente de la base de datos"
                            )
                        }
                    } else {
                        Log.d(TAG, "Base de datos no encontrada: $oldName")
                    }
                }

                // Si la base de datos actual es la que se está renombrando, actualizar el nombre
                if (success && currentDatabaseName == oldName) {
                    currentDatabaseName = newName
                }

                success
            } catch (e: Exception) {
                Log.e(TAG, "Error al renombrar base de datos: ${e.message}", e)
                false
            } finally {
                metadataRealm.close()
            }
        }

    // 5. Borrar una base de datos
    suspend fun deleteDatabase(name: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Cerrar la base de datos si está abierta
            if (currentDatabaseName == name) {
                realm?.close()
                realm = null
                currentDatabaseName = null
            }

            val metadataRealm = getMetadataRealm()
            var success = false

            try {
                // Buscar la base de datos
                val db = metadataRealm.query<DatabaseMetadata>("name == $0", name).first().find()
                if (db == null) {
                    Log.e(TAG, "Base de datos no encontrada para eliminar: $name")
                    metadataRealm.close()
                    return@withContext false
                }

                val dbId = db.id.toString()

                // Eliminar la base de datos y sus dependencias
                metadataRealm.writeBlocking {
                    try {
                        // 1. Eliminar todos los datos asociados a las colecciones de esta base de datos
                        val collections =
                            metadataRealm.query<CollectionMetadata>("databaseId == $0", dbId).find()
                        Log.d(
                            TAG,
                            "Eliminando ${collections.size} colecciones de la base de datos $name"
                        )

                        for (collection in collections) {
                            val collectionId = collection.id.toString()

                            // Eliminar datos
                            val dataItems =
                                metadataRealm.query<DynamicData>("collectionId == $0", collectionId)
                                    .find()
                            Log.d(
                                TAG,
                                "Eliminando ${dataItems.size} registros de la colección ${collection.name}"
                            )
                            for (data in dataItems) {
                                try {
                                    delete(findLatest(data) ?: data)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error al eliminar dato: ${e.message}")
                                }
                            }

                            // Eliminar campos
                            val fields = metadataRealm.query<FieldMetadata>(
                                "collectionId == $0",
                                collectionId
                            ).find()
                            Log.d(
                                TAG,
                                "Eliminando ${fields.size} campos de la colección ${collection.name}"
                            )
                            for (field in fields) {
                                try {
                                    delete(findLatest(field) ?: field)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error al eliminar campo: ${e.message}")
                                }
                            }

                            // Eliminar la colección
                            try {
                                delete(findLatest(collection) ?: collection)
                                Log.d(TAG, "Colección eliminada: ${collection.name}")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al eliminar colección: ${e.message}")
                            }
                        }

                        // 2. Finalmente eliminar la base de datos
                        try {
                            delete(findLatest(db) ?: db)
                            Log.d(TAG, "Base de datos eliminada de los metadatos: $name")
                            success = true
                        } catch (e: Exception) {
                            Log.e(
                                TAG,
                                "Error al eliminar base de datos de los metadatos: ${e.message}"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error en la transacción de eliminación: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al buscar la base de datos para eliminar: ${e.message}")
            } finally {
                metadataRealm.close()
            }

            // Intentar eliminar el archivo físico
            if (success) {
                try {
                    val dbFile = File(context.filesDir.parent, "files/$name.realm")
                    if (dbFile.exists()) {
                        val deleted = dbFile.delete()
                        Log.d(TAG, "Archivo de base de datos eliminado: $deleted")

                        // También intentar eliminar archivos relacionados
                        val dbLockFile = File(context.filesDir.parent, "files/$name.realm.lock")
                        if (dbLockFile.exists()) {
                            dbLockFile.delete()
                        }

                        val dbManagementFile =
                            File(context.filesDir.parent, "files/$name.realm.management")
                        if (dbManagementFile.exists() && dbManagementFile.isDirectory) {
                            dbManagementFile.deleteRecursively()
                        }
                    } else {
                        Log.d(TAG, "Archivo de base de datos no encontrado: $dbFile")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al eliminar archivo físico: ${e.message}")
                }
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Error general al eliminar base de datos: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // 6. Crear colecciones
    suspend fun createCollection(databaseName: String, collectionName: String): Boolean =
        withContext(Dispatchers.IO) {
            val metadataRealm = getMetadataRealm()
            try {
                var success = false

                // Verificar si la base de datos existe
                val db =
                    metadataRealm.query<DatabaseMetadata>("name == $0", databaseName).first().find()
                if (db == null) {
                    Log.d(TAG, "Base de datos no encontrada: $databaseName")
                    return@withContext false
                }

                metadataRealm.writeBlocking {
                    // Verificar si la colección ya existe
                    val existingCollection = metadataRealm.query<CollectionMetadata>(
                        "databaseId == $0 AND name == $1",
                        db.id.toString(),
                        collectionName
                    ).first().find()

                    if (existingCollection == null) {
                        val newCollection = CollectionMetadata().apply {
                            this.databaseId = db.id.toString()
                            this.name = collectionName
                        }
                        copyToRealm(newCollection)
                        success = true
                        Log.d(TAG, "Colección creada: $collectionName en $databaseName")
                    } else {
                        Log.d(TAG, "La colección ya existe: $collectionName en $databaseName")
                    }
                }

                success
            } catch (e: Exception) {
                Log.e(TAG, "Error al crear colección: ${e.message}", e)
                false
            } finally {
                metadataRealm.close()
            }
        }

    // 7. Listar colecciones
    suspend fun listCollections(databaseName: String): List<String> = withContext(Dispatchers.IO) {
        val metadataRealm = getMetadataRealm()
        try {
            val db =
                metadataRealm.query<DatabaseMetadata>("name == $0", databaseName).first().find()
            val collections = if (db != null) {
                metadataRealm.query<CollectionMetadata>("databaseId == $0", db.id.toString())
                    .sort("name", Sort.ASCENDING)
                    .find()
                    .map { it.name }
            } else {
                Log.d(TAG, "Base de datos no encontrada para listar colecciones: $databaseName")
                emptyList()
            }

            Log.d(TAG, "Colecciones encontradas: ${collections.size} en $databaseName")
            collections
        } catch (e: Exception) {
            Log.e(TAG, "Error al listar colecciones: ${e.message}", e)
            emptyList()
        } finally {
            metadataRealm.close()
        }
    }

    // 8. Modificar colecciones
    suspend fun renameCollection(databaseName: String, oldName: String, newName: String): Boolean =
        withContext(Dispatchers.IO) {
            val metadataRealm = getMetadataRealm()
            try {
                var success = false

                // Verificar si la base de datos existe
                val db =
                    metadataRealm.query<DatabaseMetadata>("name == $0", databaseName).first().find()
                if (db == null) {
                    Log.d(TAG, "Base de datos no encontrada: $databaseName")
                    return@withContext false
                }

                // Verificar si el nuevo nombre ya existe
                val newNameExists = metadataRealm.query<CollectionMetadata>(
                    "databaseId == $0 AND name == $1",
                    db.id.toString(),
                    newName
                ).first().find() != null

                if (newNameExists) {
                    Log.d(TAG, "No se puede renombrar, el nuevo nombre ya existe: $newName")
                    return@withContext false
                }

                metadataRealm.writeBlocking {
                    val collection = metadataRealm.query<CollectionMetadata>(
                        "databaseId == $0 AND name == $1",
                        db.id.toString(),
                        oldName
                    ).first().find()

                    if (collection != null) {
                        val latestCollection = findLatest(collection)
                        if (latestCollection != null) {
                            latestCollection.name = newName
                            latestCollection.lastModified = System.currentTimeMillis()
                            success = true
                            Log.d(
                                TAG,
                                "Colección renombrada: $oldName -> $newName en $databaseName"
                            )
                        } else {
                            Log.d(
                                TAG,
                                "No se pudo encontrar la versión más reciente de la colección"
                            )
                        }
                    } else {
                        Log.d(TAG, "Colección no encontrada: $oldName en $databaseName")
                    }
                }

                success
            } catch (e: Exception) {
                Log.e(TAG, "Error al renombrar colección: ${e.message}", e)
                false
            } finally {
                metadataRealm.close()
            }
        }

    // 9. Eliminar colecciones
    suspend fun deleteCollection(databaseName: String, collectionName: String): Boolean =
        withContext(Dispatchers.IO) {
            val metadataRealm = getMetadataRealm()
            try {
                var success = false

                // Buscar la base de datos
                val db =
                    metadataRealm.query<DatabaseMetadata>("name == $0", databaseName).first().find()
                if (db == null) {
                    Log.e(TAG, "Base de datos no encontrada: $databaseName")
                    return@withContext false
                }

                // Buscar la colección
                val collection = metadataRealm.query<CollectionMetadata>(
                    "databaseId == $0 AND name == $1",
                    db.id.toString(),
                    collectionName
                ).first().find()

                if (collection == null) {
                    Log.e(TAG, "Colección no encontrada: $collectionName en $databaseName")
                    return@withContext false
                }

                val collectionId = collection.id.toString()

                // Eliminar la colección y sus dependencias
                metadataRealm.writeBlocking {
                    try {
                        // 1. Eliminar todos los datos asociados a esta colección
                        val dataItems =
                            metadataRealm.query<DynamicData>("collectionId == $0", collectionId)
                                .find()
                        Log.d(
                            TAG,
                            "Eliminando ${dataItems.size} registros de la colección $collectionName"
                        )
                        for (data in dataItems) {
                            try {
                                delete(findLatest(data) ?: data)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al eliminar dato: ${e.message}")
                            }
                        }

                        // 2. Eliminar todos los campos asociados a esta colección
                        val fields =
                            metadataRealm.query<FieldMetadata>("collectionId == $0", collectionId)
                                .find()
                        Log.d(
                            TAG,
                            "Eliminando ${fields.size} campos de la colección $collectionName"
                        )
                        for (field in fields) {
                            try {
                                delete(findLatest(field) ?: field)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al eliminar campo: ${e.message}")
                            }
                        }

                        // 3. Finalmente eliminar la colección
                        try {
                            delete(findLatest(collection) ?: collection)
                            Log.d(TAG, "Colección eliminada: $collectionName")
                            success = true
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al eliminar colección: ${e.message}")
                        }
                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Error en la transacción de eliminación de colección: ${e.message}"
                        )
                    }
                }

                success
            } catch (e: Exception) {
                Log.e(TAG, "Error general al eliminar colección: ${e.message}")
                false
            } finally {
                metadataRealm.close()
            }
        }

    // 10. Crear campos
    suspend fun createField(
        databaseName: String,
        collectionName: String,
        fieldName: String,
        fieldType: String
    ): Boolean = withContext(Dispatchers.IO) {
        // Validar que el nombre del campo no sea "id"
        if (fieldName.lowercase() == "id") {
            Log.e(TAG, "No se permite crear campos con nombre 'id'")
            return@withContext false
        }

        val metadataRealm = getMetadataRealm()
        try {
            // Buscar la base de datos
            val db = metadataRealm.query<DatabaseMetadata>("name == $0", databaseName).first().find()
            if (db == null) {
                Log.e(TAG, "Base de datos no encontrada: $databaseName")
                return@withContext false
            }

            // Buscar la colección
            val collection = metadataRealm.query<CollectionMetadata>(
                "databaseId == $0 AND name == $1",
                db.id.toString(),
                collectionName
            ).first().find()

            if (collection == null) {
                Log.e(TAG, "Colección no encontrada: $collectionName en $databaseName")
                return@withContext false
            }

            val collectionId = collection.id.toString()

            // Verificar si el campo ya existe
            val existingField = metadataRealm.query<FieldMetadata>(
                "collectionId == $0 AND name == $1",
                collectionId,
                fieldName
            ).first().find()

            if (existingField != null) {
                Log.e(TAG, "El campo ya existe: $fieldName en $collectionName")
                return@withContext false
            }

            // Validar el tipo de campo
            val validTypes = listOf("STRING", "INTEGER", "DOUBLE", "BOOLEAN", "JSON")
            if (!validTypes.contains(fieldType.uppercase())) {
                Log.e(TAG, "Tipo de campo no válido: $fieldType")
                return@withContext false
            }

            // Crear el campo
            var success = false
            metadataRealm.writeBlocking {
                try {
                    val field = FieldMetadata().apply {
                        this.collectionId = collectionId
                        this.name = fieldName
                        this.type = fieldType.uppercase()
                    }
                    copyToRealm(field)
                    success = true
                } catch (e: Exception) {
                    Log.e(TAG, "Error al crear campo", e)
                }
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Error en createField", e)
            false
        } finally {
            metadataRealm.close()
        }
    }

    // 11. Listar campos
    suspend fun listFields(
        databaseName: String,
        collectionName: String
    ): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val metadataRealm = getMetadataRealm()
        try {
            val fields = mutableListOf<Pair<String, String>>()

            val db =
                metadataRealm.query<DatabaseMetadata>("name == $0", databaseName).first().find()
            if (db != null) {
                val collection = metadataRealm.query<CollectionMetadata>(
                    "databaseId == $0 AND name == $1",
                    db.id.toString(),
                    collectionName
                ).first().find()

                if (collection != null) {
                    // Obtener todos los campos definidos
                    val definedFields = metadataRealm.query<FieldMetadata>(
                        "collectionId == $0",
                        collection.id.toString()
                    )
                        .sort("name", Sort.ASCENDING)
                        .find()
                        .map { Pair(it.name, it.type) }

                    fields.addAll(definedFields)

                    // Verificar si hay campos en los datos que no están definidos
                    val dataItems = metadataRealm.query<DynamicData>(
                        "collectionId == $0",
                        collection.id.toString()
                    ).find()
                    val undefinedFields = mutableSetOf<String>()

                    for (data in dataItems) {
                        try {
                            val values = JSONObject(data.fieldValues)
                            val keys = values.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                if (!definedFields.any { it.first == key } && key != "id") {
                                    undefinedFields.add(key)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al procesar campos no definidos: ${e.message}")
                        }
                    }

                    // Crear automáticamente los campos no definidos
                    for (fieldName in undefinedFields) {
                        metadataRealm.writeBlocking {
                            val newField = FieldMetadata().apply {
                                this.collectionId = collection.id.toString()
                                this.name = fieldName
                                this.type = "String" // Tipo por defecto
                            }
                            copyToRealm(newField)
                            Log.d(TAG, "Campo creado automáticamente durante listado: $fieldName")
                        }
                        fields.add(Pair(fieldName, "String"))
                    }

                    Log.d(TAG, "Campos encontrados: ${fields.size} en $collectionName")
                } else {
                    Log.d(TAG, "Colección no encontrada: $collectionName en $databaseName")
                }
            } else {
                Log.d(TAG, "Base de datos no encontrada: $databaseName")
            }

            fields
        } catch (e: Exception) {
            Log.e(TAG, "Error al listar campos: ${e.message}", e)
            emptyList()
        } finally {
            metadataRealm.close()
        }
    }

    // 12. Modificar campos
    suspend fun updateField(
        databaseName: String,
        collectionName: String,
        oldFieldName: String,
        newFieldName: String,
        newFieldType: String
    ): Boolean = withContext(Dispatchers.IO) {
        val metadataRealm = getMetadataRealm()
        try {
            var success = false

            // Verificar si la base de datos existe
            val db =
                metadataRealm.query<DatabaseMetadata>("name == $0", databaseName).first().find()
            if (db == null) {
                Log.d(TAG, "Base de datos no encontrada: $databaseName")
                return@withContext false
            }

            // Verificar si la colección existe
            val collection = metadataRealm.query<CollectionMetadata>(
                "databaseId == $0 AND name == $1",
                db.id.toString(),
                collectionName
            ).first().find()

            if (collection == null) {
                Log.d(TAG, "Colección no encontrada: $collectionName en $databaseName")
                return@withContext false
            }

            // Verificar si el nuevo nombre ya existe (si es diferente del actual)
            if (oldFieldName != newFieldName) {
                val newNameExists = metadataRealm.query<FieldMetadata>(
                    "collectionId == $0 AND name == $1",
                    collection.id.toString(),
                    newFieldName
                ).first().find() != null

                if (newNameExists) {
                    Log.d(TAG, "No se puede renombrar, el nuevo nombre ya existe: $newFieldName")
                    return@withContext false
                }
            }

            var fieldId: String? = null

            metadataRealm.writeBlocking {
                val field = metadataRealm.query<FieldMetadata>(
                    "collectionId == $0 AND name == $1",
                    collection.id.toString(),
                    oldFieldName
                ).first().find()

                if (field != null) {
                    fieldId = field.id.toString()
                    val latestField = findLatest(field)
                    if (latestField != null) {
                        latestField.name = newFieldName
                        latestField.type = newFieldType
                        latestField.lastModified = System.currentTimeMillis()
                        success = true
                        Log.d(
                            TAG,
                            "Campo actualizado: $oldFieldName -> $newFieldName ($newFieldType)"
                        )
                    } else {
                        Log.d(TAG, "No se pudo encontrar la versión más reciente del campo")
                    }
                } else {
                    Log.d(TAG, "Campo no encontrado: $oldFieldName en $collectionName")
                }
            }

            // Actualizar los datos existentes para reflejar el cambio de nombre del campo
            if (success && fieldId != null && oldFieldName != newFieldName) {
                metadataRealm.writeBlocking {
                    val data = metadataRealm.query<DynamicData>(
                        "collectionId == $0",
                        collection.id.toString()
                    ).find()
                    Log.d(TAG, "Actualizando campo en ${data.size} registros")

                    for (item in data) {
                        try {
                            val values = JSONObject(item.fieldValues)
                            if (values.has(oldFieldName)) {
                                val value = values.get(oldFieldName)
                                values.remove(oldFieldName)
                                values.put(newFieldName, value)
                                findLatest(item)?.fieldValues = values.toString()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al actualizar campo en registro: ${e.message}", e)
                        }
                    }
                }
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar campo: ${e.message}", e)
            false
        } finally {
            metadataRealm.close()
        }
    }

    // 13. Borrar campos
    suspend fun deleteField(
        databaseName: String,
        collectionName: String,
        fieldName: String
    ): Boolean = withContext(Dispatchers.IO) {
        val metadataRealm = getMetadataRealm()
        try {
            var success = false

            // Buscar la base de datos
            val db = metadataRealm.query<DatabaseMetadata>("name == $0", databaseName).first().find()
            if (db == null) {
                Log.e(TAG, "Base de datos no encontrada: $databaseName")
                return@withContext false
            }

            // Buscar la colección
            val collection = metadataRealm.query<CollectionMetadata>(
                "databaseId == $0 AND name == $1",
                db.id.toString(),
                collectionName
            ).first().find()

            if (collection == null) {
                Log.e(TAG, "Colección no encontrada: $collectionName en $databaseName")
                return@withContext false
            }

            val collectionId = collection.id.toString()

            // Buscar el campo
            val field = metadataRealm.query<FieldMetadata>(
                "collectionId == $0 AND name == $1",
                collectionId,
                fieldName
            ).first().find()

            if (field == null) {
                Log.e(TAG, "Campo no encontrado: $fieldName en $collectionName")
                return@withContext false
            }

            // Eliminar el campo y actualizar los datos
            metadataRealm.writeBlocking {
                try {
                    // 1. Eliminar el campo de los metadatos
                    delete(findLatest(field) ?: field)
                    Log.d(TAG, "Campo eliminado de metadatos: $fieldName")

                    // 2. Eliminar el campo de TODOS los registros
                    val dataItems = metadataRealm.query<DynamicData>("collectionId == $0", collectionId).find()
                    Log.d(TAG, "Eliminando campo de ${dataItems.size} registros")

                    for (data in dataItems) {
                        try {
                            val values = JSONObject(data.fieldValues)
                            if (values.has(fieldName)) {
                                values.remove(fieldName)
                                findLatest(data)?.fieldValues = values.toString()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al eliminar campo de registro: ${e.message}")
                        }
                    }

                    success = true
                } catch (e: Exception) {
                    Log.e(TAG, "Error en la transacción de eliminación de campo: ${e.message}")
                }
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Error general al eliminar campo: ${e.message}")
            false
        } finally {
            metadataRealm.close()
        }
    }

    // 14. Insertar datos en los atributos - Sin incluir ID
    suspend fun insertData(
        databaseName: String,
        collectionName: String,
        values: Map<String, Any>
    ): Boolean = withContext(Dispatchers.IO) {
        val metadataRealm = getMetadataRealm()
        try {
            // Validar existencia de la base de datos
            val db = metadataRealm.query<DatabaseMetadata>("name == $0", databaseName).first().find()
            if (db == null) {
                Log.e(TAG, "Base de datos no encontrada: $databaseName")
                return@withContext false
            }

            // Validar existencia de la colección
            val collection = metadataRealm.query<CollectionMetadata>(
                "databaseId == $0 AND name == $1",
                db.id.toString(),
                collectionName
            ).first().find()

            if (collection == null) {
                Log.e(TAG, "Colección no encontrada: $collectionName en $databaseName")
                return@withContext false
            }

            val collectionId = collection.id.toString()

            // Obtener los campos definidos para esta colección
            val definedFields = metadataRealm.query<FieldMetadata>(
                "collectionId == $0",
                collectionId
            ).find().associate { it.name to it.type }

            // Filtrar cualquier campo "id" de los valores
            val filteredValues = values.filterKeys { it.lowercase() != "id" }

            // Validar que los valores correspondan a campos definidos
            val invalidFields = filteredValues.keys.filter { !definedFields.containsKey(it) }
            if (invalidFields.isNotEmpty()) {
                Log.e(TAG, "Campos no definidos: $invalidFields")
                return@withContext false
            }

            // Validar tipos de datos
            for ((fieldName, value) in filteredValues) {
                val fieldType = definedFields[fieldName] ?: continue

                val isValidType = when (fieldType) {
                    "STRING" -> value is String
                    "INTEGER" -> value is Int || value is Long || (value is String && value.toIntOrNull() != null)
                    "DOUBLE" -> value is Double || value is Float || (value is String && value.toDoubleOrNull() != null)
                    "BOOLEAN" -> value is Boolean || (value is String && (value.lowercase() == "true" || value.lowercase() == "false"))
                    "JSON" -> value is String || value is Map<*, *> || value is List<*>
                    else -> false
                }

                if (!isValidType) {
                    Log.e(TAG, "Tipo de dato inválido para el campo $fieldName: $value (${value.javaClass.simpleName}) no es compatible con $fieldType")
                    return@withContext false
                }
            }

            // Crear JSON con los valores filtrados y convertidos
            val jsonValues = JSONObject()
            for ((fieldName, value) in filteredValues) {
                val fieldType = definedFields[fieldName]

                // Convertir valores según sea necesario
                val convertedValue = when (fieldType) {
                    "INTEGER" -> if (value is String) value.toInt() else value
                    "DOUBLE" -> if (value is String) value.toDouble() else value
                    "BOOLEAN" -> if (value is String) value.lowercase() == "true" else value
                    "JSON" -> if (value is Map<*, *> || value is List<*>) JSONObject(value.toString()) else value
                    else -> value
                }

                jsonValues.put(fieldName, convertedValue)
            }

            // Insertar datos
            var success = false
            metadataRealm.writeBlocking {
                try {
                    val data = DynamicData().apply {
                        this.collectionId = collectionId
                        this.fieldValues = jsonValues.toString()
                    }
                    copyToRealm(data)
                    success = true
                    Log.d(TAG, "Datos insertados correctamente: $jsonValues")
                } catch (e: Exception) {
                    Log.e(TAG, "Error al insertar datos", e)
                }
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Error en insertData", e)
            false
        } finally {
            metadataRealm.close()
        }
    }

    // 15. Consultar datos - Sin incluir ID automático
    suspend fun queryData(
        databaseName: String,
        collectionName: String,
        filter: Map<String, Any>? = null
    ): List<Map<String, Any?>> = withContext(Dispatchers.IO) {
        val metadataRealm = getMetadataRealm()
        try {
            // Validar existencia
            val db = metadataRealm.query<DatabaseMetadata>("name == $0", databaseName).first().find()
                ?: return@withContext emptyList()

            val collection = metadataRealm.query<CollectionMetadata>(
                "databaseId == $0 AND name == $1",
                db.id.toString(),
                collectionName
            ).first().find() ?: return@withContext emptyList()

            val collectionId = collection.id.toString()

            // Obtener todos los campos definidos
            val definedFields = metadataRealm.query<FieldMetadata>(
                "collectionId == $0",
                collectionId
            ).find().map { it.name }

            // Consultar los datos
            val results = mutableListOf<Map<String, Any?>>()
            val dataItems = metadataRealm.query<DynamicData>("collectionId == $0", collectionId).find()

            // Crear un índice para cada registro (posición en la lista)
            for ((index, record) in dataItems.withIndex()) {
                try {
                    val values = if (record.fieldValues.isBlank()) {
                        JSONObject()
                    } else {
                        JSONObject(record.fieldValues)
                    }

                    // Construir resultado solo con los campos definidos
                    val result = mutableMapOf<String, Any?>()

                    // Usar el índice como identificador de posición (no visible en UI)
                    result["__position"] = index

                    // Asegurar que todos los campos definidos estén presentes
                    definedFields.forEach { field ->
                        // Excluir explícitamente cualquier campo llamado "id"
                        if (field.lowercase() != "id") {
                            result[field] = if (values.has(field)) {
                                val value = values.get(field)
                                if (value == JSONObject.NULL) null else value
                            } else {
                                null
                            }
                        }
                    }

                    // Aplicar filtro si existe
                    if (filter == null || filter.all { (key, value) ->
                            result.containsKey(key) && result[key].toString() == value.toString()
                        }) {
                        results.add(result)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error procesando registro", e)
                }
            }

            results
        } catch (e: Exception) {
            Log.e(TAG, "Error en queryData", e)
            emptyList()
        } finally {
            metadataRealm.close()
        }
    }
    // 16. Modificar o actualizar datos - Versión mejorada
    // Método actualizado para actualizar datos por posición o filtro
    suspend fun updateData(
        databaseName: String,
        collectionName: String,
        position: Int? = null,
        filter: Map<String, Any>? = null,
        values: Map<String, Any>
    ): Boolean = withContext(Dispatchers.IO) {
        val metadataRealm = getMetadataRealm()
        try {
            var success = false

            // Buscar la base de datos
            val db = metadataRealm.query<DatabaseMetadata>("name == $0", databaseName).first().find()
            if (db == null) {
                Log.e(TAG, "Base de datos no encontrada: $databaseName")
                return@withContext false
            }

            // Buscar la colección
            val collection = metadataRealm.query<CollectionMetadata>(
                "databaseId == $0 AND name == $1",
                db.id.toString(),
                collectionName
            ).first().find()

            if (collection == null) {
                Log.e(TAG, "Colección no encontrada: $collectionName en $databaseName")
                return@withContext false
            }

            val collectionId = collection.id.toString()

            // Obtener todos los registros
            val dataItems = metadataRealm.query<DynamicData>("collectionId == $0", collectionId)
                .find()
                .toList() // Convertir a lista para acceder por índice

            // No hay registros para actualizar
            if (dataItems.isEmpty()) {
                Log.d(TAG, "No hay registros para actualizar")
                return@withContext false
            }

            metadataRealm.writeBlocking {
                try {
                    if (position != null && position >= 0 && position < dataItems.size) {
                        // Actualizar por posición
                        val data = dataItems[position]
                        val currentValues = if (data.fieldValues.isBlank()) {
                            JSONObject()
                        } else {
                            JSONObject(data.fieldValues)
                        }

                        // Actualizar valores
                        for ((key, value) in values) {
                            currentValues.put(key, value)
                        }

                        // Guardar valores actualizados
                        findLatest(data)?.fieldValues = currentValues.toString()
                        success = true
                        Log.d(TAG, "Registro actualizado en posición: $position")
                    } else if (filter != null && filter.isNotEmpty()) {
                        // Actualizar por filtro
                        var updatedCount = 0

                        for (item in dataItems) {
                            try {
                                val currentValues = if (item.fieldValues.isBlank()) {
                                    JSONObject()
                                } else {
                                    JSONObject(item.fieldValues)
                                }

                                var matchesFilter = true

                                // Verificar si todos los criterios del filtro coinciden
                                for ((key, value) in filter) {
                                    if (!currentValues.has(key) || currentValues.get(key).toString() != value.toString()) {
                                        matchesFilter = false
                                        break
                                    }
                                }

                                if (matchesFilter) {
                                    // Actualizar valores
                                    for ((key, value) in values) {
                                        currentValues.put(key, value)
                                    }

                                    // Guardar valores actualizados
                                    findLatest(item)?.fieldValues = currentValues.toString()
                                    updatedCount++
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al procesar filtro: ${e.message}")
                            }
                        }

                        success = updatedCount > 0
                        Log.d(TAG, "Registros actualizados por filtro: $updatedCount")
                    } else {
                        Log.e(TAG, "Se requiere posición o filtro para actualizar registros")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error en la transacción de actualización de datos: ${e.message}")
                }
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Error general al actualizar registro: ${e.message}")
            false
        } finally {
            metadataRealm.close()
        }
    }

    // 17. Borrar o eliminar datos - Por posición o filtro
    suspend fun deleteData(
        databaseName: String,
        collectionName: String,
        position: Int? = null,
        filter: Map<String, Any>? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val metadataRealm = getMetadataRealm()
        try {
            var success = false

            // Buscar la base de datos
            val db = metadataRealm.query<DatabaseMetadata>("name == $0", databaseName).first().find()
            if (db == null) {
                Log.e(TAG, "Base de datos no encontrada: $databaseName")
                return@withContext false
            }

            // Buscar la colección
            val collection = metadataRealm.query<CollectionMetadata>(
                "databaseId == $0 AND name == $1",
                db.id.toString(),
                collectionName
            ).first().find()

            if (collection == null) {
                Log.e(TAG, "Colección no encontrada: $collectionName en $databaseName")
                return@withContext false
            }

            val collectionId = collection.id.toString()

            // Obtener todos los registros
            val dataItems = metadataRealm.query<DynamicData>("collectionId == $0", collectionId)
                .find()
                .toList() // Convertir a lista para acceder por índice

            // No hay registros para eliminar
            if (dataItems.isEmpty()) {
                Log.d(TAG, "No hay registros para eliminar")
                return@withContext false
            }

            metadataRealm.writeBlocking {
                try {
                    if (position != null && position >= 0 && position < dataItems.size) {
                        // Eliminar por posición
                        val data = dataItems[position]
                        delete(findLatest(data) ?: data)
                        success = true
                        Log.d(TAG, "Registro eliminado en posición: $position")
                    } else if (filter != null && filter.isNotEmpty()) {
                        // Eliminar por filtro
                        var deletedCount = 0

                        for (item in dataItems) {
                            try {
                                val values = JSONObject(item.fieldValues)
                                var matchesFilter = true

                                // Verificar si todos los criterios del filtro coinciden
                                for ((key, value) in filter) {
                                    if (!values.has(key) || values.get(key).toString() != value.toString()) {
                                        matchesFilter = false
                                        break
                                    }
                                }

                                if (matchesFilter) {
                                    delete(findLatest(item) ?: item)
                                    deletedCount++
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al procesar filtro: ${e.message}")
                            }
                        }

                        success = deletedCount > 0
                        Log.d(TAG, "Registros eliminados por filtro: $deletedCount")
                    } else {
                        Log.e(TAG, "Se requiere posición o filtro para eliminar registros")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error en la transacción de eliminación de datos: ${e.message}")
                }
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Error general al eliminar registro: ${e.message}")
            false
        } finally {
            metadataRealm.close()
        }
    }

    // Método para cerrar la base de datos
    fun closeDatabase() {
        try {
            realm?.close()
            realm = null
            currentDatabaseName = null
            Log.d(TAG, "Base de datos cerrada")
        } catch (e: Exception) {
            Log.e(TAG, "Error al cerrar base de datos: ${e.message}", e)
        }
    }
    suspend fun syncCollectionFields(
        databaseName: String,
        collectionName: String
    ): Boolean = withContext(Dispatchers.IO) {
        val metadataRealm = getMetadataRealm()
        try {
            var success = true

            // Buscar la base de datos
            val db = metadataRealm.query<DatabaseMetadata>("name == $0", databaseName).first().find()
            if (db == null) {
                Log.e(TAG, "Base de datos no encontrada: $databaseName")
                return@withContext false
            }

            // Buscar la colección
            val collection = metadataRealm.query<CollectionMetadata>(
                "databaseId == $0 AND name == $1",
                db.id.toString(),
                collectionName
            ).first().find()

            if (collection == null) {
                Log.e(TAG, "Colección no encontrada: $collectionName")
                return@withContext false
            }

            val collectionId = collection.id.toString()

            metadataRealm.writeBlocking {
                // Obtener todos los campos definidos
                val definedFields = query<FieldMetadata>("collectionId == $0", collectionId)
                    .find()
                    .map { it.name }

                // Procesar todos los registros
                val dataItems = query<DynamicData>("collectionId == $0", collectionId).find()
                for (data in dataItems) {
                    try {
                        val values = if (data.fieldValues.isBlank()) {
                            JSONObject()
                        } else {
                            JSONObject(data.fieldValues)
                        }

                        var modified = false

                        // 1. Asegurar que todos los campos definidos existan
                        for (field in definedFields) {
                            if (!values.has(field)) {
                                values.put(field, JSONObject.NULL)
                                modified = true
                            }
                        }

                        // 2. Eliminar campos no definidos (opcional)
                        val keysToRemove = mutableListOf<String>()
                        val keys = values.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            if (key != "id" && !definedFields.contains(key)) {
                                keysToRemove.add(key)
                            }
                        }

                        keysToRemove.forEach {
                            values.remove(it)
                            modified = true
                        }

                        if (modified) {
                            findLatest(data)?.fieldValues = values.toString()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sincronizando registro: ${e.message}")
                        success = false
                    }
                }
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Error en sincronización: ${e.message}")
            false
        } finally {
            metadataRealm.close()
        }
    }

    suspend fun resetConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            realm?.close()
            realm = null

            kotlinx.coroutines.delay(500)

            val result = currentDatabaseName?.let { dbName ->
                openDatabase(dbName)
            } ?: true

            Log.d(TAG, "Conexión a Realm reiniciada")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error al reiniciar conexión a Realm: ${e.message}", e)
            false
        }
    }
}