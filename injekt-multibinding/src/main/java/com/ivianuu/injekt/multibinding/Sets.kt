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


package com.ivianuu.injekt.multibinding

import com.ivianuu.injekt.*
import java.util.*
import kotlin.reflect.KClass

/**
 * Attribute key for [BeanDefinition] which contains any [Set] binding of the definition
 */
const val KEY_SET_BINDINGS = "setBindings"

/**
 * Declares a empty set binding with the name [setName]
 * This is useful for retrieving a [MultiBindingSet] even if no [BeanDefinition] was bound into it
 */
fun <T : Any> ModuleContext.setBinding(setName: String) {
    factory(name = setName, override = true) {
        MultiBindingSet<T>(
            emptySet()
        )
    }
}

/**
 * Binds this [BeanDefinition] into a [Set] named [setName]
 */
infix fun <T : Any, S : T> BeanDefinition<S>.intoSet(setName: String) = apply {
    attributes.getOrSet(KEY_SET_BINDINGS) { mutableSetOf<String>() }.add(setName)

    moduleContext.factory(name = setName, override = true) {
        component.beanRegistry
            .getAllDefinitions()
            .filter { it.attributes.get<Set<String>>(KEY_SET_BINDINGS)?.contains(setName) == true }
            .map { it as BeanDefinition<T> }
            .toSet()
            .let { MultiBindingSet(it) }
    }
}

/**
 * Binds a already existing [BeanDefinition] into a [Set] named [setName]
 */
inline fun <reified T : Any, reified S : T> ModuleContext.bindIntoSet(
    setName: String,
    declarationName: String? = null
) = bindIntoSet(T::class, S::class, setName, declarationName)

/**
 * Binds a already existing [BeanDefinition] into a [Set] named [setName]
 */
fun <T : Any, S : T> ModuleContext.bindIntoSet(
    setType: KClass<T>,
    implementationType: KClass<S>,
    setName: String,
    implementationName: String? = null
) =
    factory(setType, UUID.randomUUID().toString()) {
        get(implementationType, implementationName)
    } intoSet setName