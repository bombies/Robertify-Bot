package main.utils.internal.delegates

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class SynchronizedProperty<T>(private val consumer: () -> T) : ReadOnlyProperty<Any?, T> {
    companion object {
        private val logger = LoggerFactory.getLogger(Companion::class.java)
        private val mutex = Mutex()
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return runBlocking {
            mutex.withLock {
                return@runBlocking consumer()
            }
        }
    }
}