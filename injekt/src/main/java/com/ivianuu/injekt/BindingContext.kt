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

data class BindingContext<T>(
    val binding: Binding<T>,
    val key: Key,
    val override: Boolean,
    val moduleBuilder: ModuleBuilder
)

inline fun <reified T> BindingContext<*>.bindAlias(
    name: Qualifier? = null,
    override: Boolean = false
) {
    bindAlias(T::class, name, override)
}

fun BindingContext<*>.bindAlias(
    type: KClass<*>,
    name: Qualifier? = null,
    override: Boolean = false
) {
    moduleBuilder.add(binding, type, name, override)
}

inline fun <reified T> BindingContext<*>.bindType() {
    bindAlias(T::class)
}

infix fun <T> BindingContext<T>.bindType(type: KClass<*>): BindingContext<T> {
    bindAlias(type)
    return this
}

fun <T> BindingContext<T>.bindTypes(vararg types: KClass<*>): BindingContext<T> {
    types.forEach { bindType(it) }
    return this
}

infix fun <T> BindingContext<T>.bindTypes(types: Iterable<KClass<*>>): BindingContext<T> {
    types.forEach { bindType(it) }
    return this
}

inline fun <reified T> BindingContext<*>.bindClass() {
    bindClass(T::class)
}

infix fun <T> BindingContext<T>.bindClass(clazz: KClass<*>): BindingContext<T> {
    bindAlias(clazz)
    return this
}

fun <T> BindingContext<T>.bindClasses(vararg classes: KClass<*>): BindingContext<T> {
    classes.forEach { bindClass(it) }
    return this
}

infix fun <T> BindingContext<T>.bindClasses(classes: Iterable<KClass<*>>): BindingContext<T> {
    classes.forEach { bindClass(it) }
    return this
}

infix fun <T> BindingContext<T>.bindName(name: Qualifier): BindingContext<T> {
    bindAlias(key.type.kotlin, name)
    return this
}

fun <T> BindingContext<T>.bindNames(vararg names: Qualifier): BindingContext<T> {
    names.forEach { bindName(it) }
    return this
}

infix fun <T> BindingContext<T>.bindNames(names: Iterable<Qualifier>): BindingContext<T> {
    names.forEach { bindName(it) }
    return this
}

inline fun <T : V, reified K, reified V> BindingContext<T>.bindIntoMap(
    entryKey: K,
    mapName: Qualifier? = null,
    override: Boolean = false
): BindingContext<T> = bindIntoMap(K::class, V::class, entryKey, mapName, override)

fun <T : V, K, V> BindingContext<T>.bindIntoMap(
    mapKeyType: KClass<*>,
    mapValueType: KClass<*>,
    entryKey: K,
    mapName: Qualifier? = null,
    override: Boolean = false
): BindingContext<T> {
    moduleBuilder.addBindingIntoMap(mapKeyType, mapValueType, entryKey, binding, mapName, override)
    return this
}

inline fun <T : E, reified E> BindingContext<T>.bindIntoSet(
    setName: Qualifier? = null,
    override: Boolean = false
): BindingContext<T> = bindIntoSet(E::class, setName, override)

fun <T : E, E> BindingContext<T>.bindIntoSet(
    setElementType: KClass<*>,
    setName: Qualifier? = null,
    override: Boolean = false
): BindingContext<T> {
    moduleBuilder.addBindingIntoSet(setElementType, binding, setName, override)
    return this
}