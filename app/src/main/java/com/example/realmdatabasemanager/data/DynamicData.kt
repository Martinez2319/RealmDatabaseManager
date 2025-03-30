package com.example.realmdatabasemanager.data

data class DynamicData(
    val id: String,
    val values: MutableMap<String, Any?> = mutableMapOf()
)