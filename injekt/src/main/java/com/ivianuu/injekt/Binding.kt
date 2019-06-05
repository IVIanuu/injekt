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

interface Binding<T> {
    fun link(linker: Linker) {
    }

    fun get(parameters: ParametersDefinition? = null): T

    operator fun invoke(parameters: ParametersDefinition? = null): T = get(parameters)
}

// todo remove
interface AttachAware {
    fun attached()
}

/**
class Binding<T> internal constructor(
val kind: Kind,
val type: Type<T>,
val name: Qualifier?,
val scope: Scope?,
val override: Boolean,
val definition: Definition<T>
) {

val key = Key(type, name)

val additionalKeys = mutableListOf<Key>()

val mapBindings = mutableMapOf<Key, MapBinding<*, *>>()
val setBindings = mutableMapOf<Key, SetBinding<*>>()

override fun toString(): String {
return "$kind(" +
"type=$type, " +
"name=$name)"
}

}

inline fun <reified T> binding(
kind: Kind,
name: Qualifier? = null,
scope: Scope? = null,
override: Boolean = false,
noinline definition: Definition<T>
): Binding<T> = binding(kind, typeOf(), name, scope, override, definition)

fun <T> binding(
kind: Kind,
type: Type<T>,
name: Qualifier? = null,
scope: Scope? = null,
override: Boolean = false,
definition: Definition<T>
): Binding<T> = Binding(kind, type, name, scope, override, definition)

fun <T> Binding<T>.additionalKeys(vararg keys: Key): Binding<T> {
additionalKeys.addAll(keys)
return this
}

infix fun <T> Binding<T>.additionalKeys(keys: Iterable<Key>): Binding<T> {
additionalKeys.addAll(keys)
return this
}

infix fun <T> Binding<T>.additionalKey(key: Key): Binding<T> {
additionalKeys.add(key)
return this
}

inline fun <reified T> Binding<*>.bindType() {
bindType(typeOf<T>())
}

infix fun <T> Binding<T>.bindType(type: Type<*>): Binding<T> =
additionalKey(Key(type))

fun <T> Binding<T>.bindTypes(vararg types: Type<*>): Binding<T> {
types.forEach { bindType(it) }
return this
}

infix fun <T> Binding<T>.bindType(type: KClass<*>): Binding<T> =
bindType(typeOf<Any?>(type))

fun <T> Binding<T>.bindTypes(vararg types: KClass<*>): Binding<T> {
types.forEach { bindType(it) }
return this
}

infix fun <T> Binding<T>.bindTypes(types: Iterable<KClass<*>>): Binding<T> {
types.forEach { bindTypes(it) }
return this
}

infix fun <T> Binding<T>.bindName(name: Qualifier): Binding<T> =
additionalKey(Key(type, name))

fun <T> Binding<T>.bindNames(vararg names: Qualifier): Binding<T> {
names.forEach { bindName(it) }
return this
}

infix fun <T> Binding<T>.bindNames(names: Iterable<Qualifier>): Binding<T> {
names.forEach { bindName(it) }
return this
}

inline fun <reified T> Binding<*>.bindAlias(name: Qualifier) {
bindAlias(typeOf<T>(), name)
}

fun <T> Binding<T>.bindAlias(type: Type<*>, name: Qualifier): Binding<T> =
additionalKey(Key(type, name))

infix fun <T> Binding<T>.bindAlias(pair: Pair<Type<*>, Qualifier>): Binding<T> {
bindAlias(pair.first, pair.second)
return this
}

infix fun <T : V, K, V> Binding<T>.bindIntoMap(mapBinding: MapBinding<K, V>): Binding<T> {
mapBindings[mapBinding.mapKey] = mapBinding
return this
}

inline fun <reified T : V, reified K, reified V> Binding<T>.bindIntoMap(
key: K,
keyType: Type<K> = typeOf(),
valueType: Type<V> = typeOf(),
mapName: Qualifier? = null,
override: Boolean = false
): Binding<T> {
bindIntoMap(mapBinding(key, keyType, valueType, mapName, override))
return this
}

infix fun <T : V, V> Binding<T>.bindIntoSet(setBinding: SetBinding<V>): Binding<T> {
setBindings[setBinding.setKey] = setBinding
return this
}

inline fun <reified T : V, reified V> Binding<T>.bindIntoSet(
setType: Type<T> = typeOf(),
setName: Qualifier? = null,
override: Boolean = false
): Binding<T> {
bindIntoSet(setBinding(setType, setName, override))
return this
}*/