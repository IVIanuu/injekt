package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Module
import kotlin.reflect.KClass

object ModuleRegistry {

    private val modules = mutableMapOf<KClass<*>, MutableList<Module>>()

    fun register(scope: KClass<*>, module: Module) {
        modules.getOrPut(scope) { mutableListOf() } += module
    }

    fun getForScope(scope: KClass<*>): List<Module> = modules.getOrElse(scope) { emptyList() }

}
