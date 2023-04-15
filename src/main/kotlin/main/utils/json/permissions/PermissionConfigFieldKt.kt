package main.utils.json.permissions

import main.utils.json.GenericJSONFieldKt

enum class PermissionConfigFieldKt(private val str: String) : GenericJSONFieldKt {
    USER_PERMISSIONS("users");

    override fun toString(): String = str
}