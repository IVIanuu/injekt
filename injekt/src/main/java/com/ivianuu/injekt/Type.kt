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

/**
 * Describes a injected [Type]
 */
class Type<T> internal constructor(
    val raw: KClass<*>,
    val isNullable: Boolean,
    val parameters: Array<Type<*>>
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Type<*>) return false

        if (raw != other.raw) return false
        if (isNullable != other.isNullable) return false
        if (!parameters.contentEquals(other.parameters)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = raw.hashCode()
        result = 31 * result + isNullable.hashCode()
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

        return "${raw.java.name}${if (isNullable) "?" else ""}$params"
    }

}

@UseExperimental(ExperimentalStdlibApi::class)
inline fun <reified T> typeOf(): Type<T> = kotlin.reflect.typeOf<T>().asType()

@PublishedApi
internal fun <T> KType.asType(): Type<T> =
    Type<T>(
        classifier as KClass<*>,
        isMarkedNullable,
        arguments.map { it.type!!.asType<Any?>() }.toTypedArray()
    )

fun <T> typeOf(raw: KClass<*>, vararg parameters: Type<*>): Type<T> =
    typeOf(raw, false, *parameters)

fun <T> typeOf(raw: KClass<*>, isNullable: Boolean, vararg parameters: Type<*>): Type<T> =
    Type(raw, isNullable, parameters as Array<Type<*>>)

inline fun <reified T> lazyTypeOf(): Type<Lazy<T>> =
    typeOf(Lazy::class, typeOf<T>())

inline fun <reified T> listTypeOf(): Type<List<T>> =
    typeOf(List::class, typeOf<T>())

inline fun <reified K, reified V> mapTypeOf(): Type<Map<K, V>> =
    typeOf(
        Map::class,
        typeOf<K>(),
        typeOf<V>()
    )

inline fun <reified T> providerTypeOf(): Type<Provider<T>> =
    typeOf(Provider::class, typeOf<T>())

inline fun <reified T> setTypeOf(): Type<Set<T>> =
    typeOf(Set::class, typeOf<T>())