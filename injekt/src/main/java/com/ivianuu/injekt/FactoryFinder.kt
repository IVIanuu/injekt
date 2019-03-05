/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Finds [BindingFactory]
 */
interface FactoryFinder {

    /**
     * Returns the [BindingFactory] for [type] or null
     */
    fun <T> find(type: KClass<*>): BindingFactory<T>?

}

/**
 * Default factory finder which try's to return generated [BindingFactory]s
 */
class DefaultFactoryFinder : FactoryFinder {

    private val factories = mutableMapOf<KClass<*>, BindingFactory<*>>()
    private val failedTypes = hashSetOf<KClass<*>>()

    override fun <T> find(type: KClass<*>): BindingFactory<T>? {
        if (failedTypes.contains(type)) return null

        return try {
            val factoryType = Class.forName(type.java.name + "__Factory")
            val factory = factoryType.newInstance() as BindingFactory<T>
            factories[type] = factory
            InjektPlugins.logger?.info("Found binding factory for ${type.java.name}")
            factory
        } catch (e: Exception) {
            failedTypes.add(type)
            null
        }
    }

}