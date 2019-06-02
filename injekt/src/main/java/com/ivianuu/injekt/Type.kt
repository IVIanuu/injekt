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
import kotlin.reflect.KType

// todo support nullable

class Type<T> internal constructor(
    val raw: KClass<*>,
    val parameters: Array<Type<*>>
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Type<*>) return false

        if (raw != other.raw) return false
        if (!parameters.contentEquals(other.parameters)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = raw.hashCode()
        result = 31 * result + parameters.contentHashCode()
        return result
    }

    override fun toString(): String {
        val params = if (parameters.isNotEmpty()) {
            parameters.joinToString(
                separator = ", ",
                prefix = "<",
                postfix = ">"
            ) { it.toString() }
        } else {
            ""
        }

        return "${raw.java.name}$params"
    }

}

@UseExperimental(ExperimentalStdlibApi::class)
inline fun <reified T> typeOf(): Type<T> = kotlin.reflect.typeOf<T>().asType()

@PublishedApi
internal fun <T> KType.asType(): Type<T> =
    Type<T>(classifier as KClass<*>, arguments.map { it.type!!.asType<Any?>() }.toTypedArray())

inline fun <reified T> customTypeOf(vararg parameters: Type<*>): Type<T> =
    customTypeOf(T::class, *parameters)

fun <T> customTypeOf(raw: KClass<*>, vararg parameters: Type<*>): Type<T> =
    Type<T>(raw, parameters as Array<Type<*>>)

inline fun <reified T> lazyTypeOf(): Type<Lazy<T>> =
    customTypeOf(Lazy::class, typeOf<T>())

inline fun <reified T> listTypeOf(): Type<List<T>> =
    customTypeOf(List::class, typeOf<T>())

inline fun <reified K, reified V> mapTypeOf(): Type<Map<K, V>> =
    customTypeOf(Map::class, typeOf<K>(), typeOf<V>())

inline fun <reified T> providerTypeOf(): Type<Provider<T>> =
    customTypeOf(Provider::class, typeOf<T>())

inline fun <reified T> setTypeOf(): Type<Set<T>> =
    customTypeOf(Set::class, typeOf<T>())