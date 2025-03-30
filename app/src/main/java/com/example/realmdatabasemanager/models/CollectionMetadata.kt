// Ruta: app/src/main/java/com/example/realmdatabasemanager/models/CollectionMetadata.kt
package com.example.realmdatabasemanager.models

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class CollectionMetadata : RealmObject {
    @PrimaryKey
    var id: ObjectId = ObjectId()
    var databaseId: String = ""
    var name: String = ""
    var createdAt: Long = System.currentTimeMillis()
    var lastModified: Long = System.currentTimeMillis()
}