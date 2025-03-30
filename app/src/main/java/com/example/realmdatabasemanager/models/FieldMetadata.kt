// Ruta: app/src/main/java/com/example/realmdatabasemanager/models/FieldMetadata.kt
package com.example.realmdatabasemanager.models

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class FieldMetadata : RealmObject {
    @PrimaryKey
    var id: ObjectId = ObjectId()
    var collectionId: String = ""
    var name: String = ""
    var type: String = "String" // String, Int, Double, Boolean, etc.
    var createdAt: Long = System.currentTimeMillis()
    var lastModified: Long = System.currentTimeMillis()
}