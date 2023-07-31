package main.utils.json.permissions

import main.utils.json.GenericJSONField

enum class PermissionConfigField(private val str: String) : GenericJSONField {
    USER_PERMISSIONS("users");

    override fun toString(): String = str
}