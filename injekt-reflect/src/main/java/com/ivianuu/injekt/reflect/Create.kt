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

package com.ivianuu.injekt.reflect

import com.ivianuu.injekt.*
import kotlin.reflect.KClass

/**
 * Provides the dependency via a factory. A new instance will be created every time the dependency is requested.
 */
inline fun <reified T : Any> Module.factory(
    name: String? = null,
    override: Boolean = false
) = factory(name, override) { create<T>() }

/**
 * Provides the dependency via a factory. A new instance will be created every time the dependency is requested.
 */
fun <T : Any> Module.factory(
    clazz: KClass<T>,
    name: String? = null,
    override: Boolean = false
) = factory(clazz, name, override) { create(clazz) }

/**
 * Create a Single definition for given kind T
 */
inline fun <reified T : Any> Module.single(
    name: String? = null,
    override: Boolean = false,
    createOnStart: Boolean = false
) = single(name, override, createOnStart) { create<T>() }

/**
 * Create a Single definition for given kind T
 */
fun <T : Any> Module.single(
    clazz: KClass<T>,
    name: String? = null,
    override: Boolean = false,
    createOnStart: Boolean = false
) = single(clazz, name, override, createOnStart) { create(clazz, it) }

/**
 * Create instance for kind T and inject dependencies into 1st constructor
 */
inline fun <reified T : Any> Module.create(
    params: Parameters = emptyParameters()
) = create(T::class, params)

/**
 * Create instance for type T and inject dependencies into 1st constructor.
 * The first constructor dependencies will be searched in [params] and in the other stored definitions.
 * In parameters of the same type, order matters in the object creation, so they should have the same order as they are in the primary constructor.
 */
fun <T : Any> Module.create(
    clazz: KClass<T>,
    params: Parameters = emptyParameters()
): T {
    //creating mutable paramsArray
    val paramsArray = ArrayList<Any>().apply {
        addAll(params.values.toMutableList().filterNotNull())
    }
    val ctor = clazz.java.constructors.firstOrNull() ?: error("No constructor found for class '$clazz'")
    val args = ctor.parameterTypes.map { clz ->
        //first, look in params
        val index = paramsArray.indexOfFirst {
            //checking for [class.java], [class.javaPrimitiveType] and if arg is an instance of param
            it::class.java == clz || it::class.javaPrimitiveType == clz || clz.isInstance(it)
        }
        if (index != -1) {
            //when resolving the argument from params always remove it from paramsArray
            paramsArray.removeAt(index)
        } else {
            //if not founded in params, then look in definitions
            get(clz.kotlin) { params }
        }
    }.toTypedArray()
    return ctor.newInstance(*args) as T
}

inline fun <reified R : Any, reified T : R> Module.factoryBy(
    name: String? = null,
    override: Boolean = false
) = factory(name, override) { create<T>() as R }

inline fun <reified R : Any, reified T : R> Module.singleBy(
    name: String? = null,
    override: Boolean = false,
    createOnStart: Boolean = false
) = single(name, override, createOnStart) { create<T>() as R }