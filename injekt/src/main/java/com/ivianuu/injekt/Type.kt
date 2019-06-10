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

import java.lang.reflect.ParameterizedType
import java.lang.reflect.WildcardType
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Describes a injected [Type]
 */
class Type<T> internal constructor(
    val raw: KClass<*>,
    val isNullable: Boolean,
    val parameters: Array<out Type<*>>
) {

    val rawJava = raw.java

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Type<*>) return false

        if (rawJava != other.rawJava) return false
        if (isNullable != other.isNullable) return false
        if (!parameters.contentEquals(other.parameters)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rawJava.hashCode()
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

        return "${rawJava.name}${if (isNullable) "?" else ""}$params"
    }

}

@UseExperimental(ExperimentalStdlibApi::class)
inline fun <reified T> typeOf(): Type<T> = kotlin.reflect.typeOf<T>().asType()

@PublishedApi
internal fun <T> KType.asType(): Type<T> {
    val parameters = arrayOfNulls<Type<*>>(arguments.size)
    arguments.forEachIndexed { i, kType ->
        parameters[i] = kType.type!!.asType<Any?>()
    }

    return Type<T>(
        classifier as KClass<*>,
        isMarkedNullable,
        parameters as Array<out Type<*>>
    )
}

fun <T> typeOf(raw: KClass<*>): Type<T> = Type(raw, false, emptyArray())

fun <T> typeOf(raw: KClass<*>, vararg parameters: Type<*>): Type<T> =
    Type(raw, false, parameters)

fun <T> typeOf(raw: KClass<*>, isNullable: Boolean): Type<T> =
    Type(raw, isNullable, emptyArray())

fun <T> typeOf(raw: KClass<*>, isNullable: Boolean, vararg parameters: Type<*>): Type<T> =
    Type(raw, isNullable, parameters)

fun <T> typeOf(type: java.lang.reflect.Type, isNullable: Boolean = false): Type<T> {
    if (type is WildcardType) {
        if (type.upperBounds.isNotEmpty()) {
            return typeOf(type.upperBounds.first(), isNullable)
        } else if (type.lowerBounds.isNotEmpty()) {
            return typeOf(type.lowerBounds.first(), isNullable)
        }
    }

    if (type !is ParameterizedType) {
        return Type((type as Class<*>).kotlin, isNullable, emptyArray())
    }

    return Type<T>(
        (type.rawType as Class<*>).kotlin,
        isNullable,
        (type as? ParameterizedType)
            ?.actualTypeArguments
            ?.map { typeOf<Any?>(it) }
            ?.toTypedArray()
            ?: emptyArray()
    )
}