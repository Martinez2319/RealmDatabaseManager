// Ruta: app/src/main/java/com/example/realmdatabasemanager/models/DynamicData.kt
package com.example.realmdatabasemanager.models

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class DynamicData : RealmObject {
    @PrimaryKey
    var id: ObjectId = ObjectId()
    var collectionId: String = ""
    var fieldValues: String = "" // JSON string con los valores de los campos
    var createdAt: Long = System.currentTimeMillis()
    var lastModified: Long = System.currentTimeMillis()
}