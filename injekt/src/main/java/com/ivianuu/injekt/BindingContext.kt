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
    name: Any? = null,
    override: Boolean = false
) {
    bindAlias(typeOf<T>(), name, override)
}

fun BindingContext<*>.bindAlias(
    type: Type<*>,
    name: Any? = null,
    override: Boolean = false
) {
    moduleBuilder.bind(binding as Binding<Any?>, type as Type<Any?>, name, override)
}

inline fun <reified T> BindingContext<*>.bindType() {
    bindAlias(typeOf<T>())
}

infix fun <T> BindingContext<T>.bindType(type: Type<*>): BindingContext<T> {
    bindAlias(type)
    return this
}

fun <T> BindingContext<T>.bindTypes(vararg types: Type<*>): BindingContext<T> {
    types.forEach { bindType(it) }
    return this
}

infix fun <T> BindingContext<T>.bindTypes(types: Iterable<Type<*>>): BindingContext<T> {
    types.forEach { bindType(it) }
    return this
}

inline fun <reified T> BindingContext<*>.bindClass() {
    bindClass(T::class)
}

infix fun <T> BindingContext<T>.bindClass(clazz: KClass<*>): BindingContext<T> {
    bindAlias(typeOf<Any?>(clazz))
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

infix fun <T> BindingContext<T>.bindName(name: Any): BindingContext<T> {
    bindAlias(key.type, name)
    return this
}

fun <T> BindingContext<T>.bindNames(vararg names: Any): BindingContext<T> {
    names.forEach { bindName(it) }
    return this
}

infix fun <T> BindingContext<T>.bindNames(names: Iterable<Any>): BindingContext<T> {
    names.forEach { bindName(it) }
    return this
}

inline fun <T : V, reified K, reified V> BindingContext<T>.bindIntoMap(
    entryKey: K,
    mapName: Any? = null,
    override: Boolean = false
): BindingContext<T> = bindIntoMap(typeOf<K>(), typeOf<V>(), entryKey, mapName, override)

fun <T : V, K, V> BindingContext<T>.bindIntoMap(
    mapKeyType: Type<K>,
    mapValueType: Type<V>,
    entryKey: K,
    mapName: Any? = null,
    override: Boolean = false
): BindingContext<T> {
    moduleBuilder.addBindingIntoMap(mapKeyType, mapValueType, entryKey, binding, mapName, override)
    return this
}

inline fun <T : E, reified E> BindingContext<T>.bindIntoSet(
    setName: Any? = null,
    override: Boolean = false
): BindingContext<T> = bindIntoSet(typeOf<E>(), setName, override)

fun <T : E, E> BindingContext<T>.bindIntoSet(
    setElementType: Type<E>,
    setName: Any? = null,
    override: Boolean = false
): BindingContext<T> {
    moduleBuilder.addBindingIntoSet(setElementType, binding, setName, override)
    return this
}