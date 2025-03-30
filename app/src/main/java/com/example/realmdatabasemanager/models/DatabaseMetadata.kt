// Ruta: app/src/main/java/com/example/realmdatabasemanager/models/DatabaseMetadata.kt
package com.example.realmdatabasemanager.models

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class DatabaseMetadata : RealmObject {
    @PrimaryKey
    var id: ObjectId = ObjectId()
    var name: String = ""
    var createdAt: Long = System.currentTimeMillis()
    var lastModified: Long = System.currentTimeMillis()
}