package com.ivianuu.injekt.internal

import com.ivianuu.injekt.ComponentDsl
import kotlin.reflect.KClass

object ModuleRegistry {

    private val modules = mutableMapOf<KClass<*>, MutableList<ComponentDsl.() -> Unit>>()

    fun register(scope: KClass<*>, module: ComponentDsl.() -> Unit) {
        modules.getOrPut(scope) { mutableListOf() } += module
    }

    fun getForScope(scope: KClass<*>): List<ComponentDsl.() -> Unit> =
        modules.getOrElse(scope) { emptyList() }

}
