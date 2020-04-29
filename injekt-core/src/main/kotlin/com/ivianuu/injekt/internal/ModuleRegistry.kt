package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Module
import org.jetbrains.annotations.TestOnly
import kotlin.reflect.KClass

object ModuleRegistry {

    private val modules = mutableMapOf<KClass<*>, MutableList<@Module () -> Unit>>()

    fun register(scope: KClass<*>, module: @Module () -> Unit) {
        modules.getOrPut(scope) { mutableListOf() } += module
    }

    fun getForScope(scope: KClass<*>): List<@Module () -> Unit> =
        modules.getOrElse(scope) { emptyList() }

    @TestOnly
    fun clear() {
        modules.clear()
    }

}
