package main.utils.internal.delegates

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ImmutableListGetDelegate<T> : ReadWriteProperty<Any?, List<T>> {
    var field: List<T> = ArrayList()

    override fun getValue(thisRef: Any?, property: KProperty<*>): List<T> {
        return field.toList()
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: List<T>) {
        field = value
    }

}