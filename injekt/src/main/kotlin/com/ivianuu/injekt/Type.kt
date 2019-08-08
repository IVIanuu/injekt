/*
 * Copyright 2019 Manuel Wrage
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
import kotlin.jvm.internal.ClassBasedDeclarationContainer
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

inline fun <reified T> typeOf(): Type<T> = kotlin.reflect.typeOf<T>().asType()

inline fun <T> typeOf(instance: T): Type<T> = typeOf<T>((instance as Any)::class)

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

fun <T> typeOf(raw: KClass<*>): Type<T> {
    val finalRaw = unboxed(raw)
    return Type(finalRaw, false, emptyArray())
}

fun <T> typeOf(raw: KClass<*>, vararg parameters: Type<*>): Type<T> =
    Type(raw, false, parameters)

fun <T> typeOf(raw: KClass<*>, isNullable: Boolean): Type<T> {
    val finalRaw = if (isNullable) boxed(raw) else unboxed(raw)
    return Type(finalRaw, isNullable, emptyArray())
}

fun <T> typeOf(raw: KClass<*>, isNullable: Boolean, vararg parameters: Type<*>): Type<T> {
    return Type(raw, isNullable, parameters)
}

fun <T> typeOf(type: java.lang.reflect.Type, isNullable: Boolean = false): Type<T> {
    if (type is WildcardType) {
        if (type.upperBounds.isNotEmpty()) {
            return typeOf(type.upperBounds.first(), isNullable)
        } else if (type.lowerBounds.isNotEmpty()) {
            return typeOf(type.lowerBounds.first(), isNullable)
        }
    }

    if (type is ParameterizedType) {
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

    var kotlinType = (type as Class<*>).kotlin

    kotlinType = if (isNullable) {
        boxed(kotlinType)
    } else {
        unboxed(kotlinType)
    }

    return Type(kotlinType, isNullable, emptyArray())
}

private fun unboxed(type: KClass<*>): KClass<*> {
    val thisJClass = (type as ClassBasedDeclarationContainer).jClass
    if (thisJClass.isPrimitive) return type

    return when (thisJClass.name) {
        "java.lang.Boolean" -> Boolean::class
        "java.lang.Character" -> Char::class
        "java.lang.Byte" -> Byte::class
        "java.lang.Short" -> Short::class
        "java.lang.Integer" -> Int::class
        "java.lang.Float" -> Float::class
        "java.lang.Long" -> Long::class
        "java.lang.Double" -> Double::class
        else -> type
    }
}

private fun boxed(type: KClass<*>): KClass<*> {
    val jClass = (type as ClassBasedDeclarationContainer).jClass
    if (!jClass.isPrimitive) return type

    return when (jClass.name) {
        "boolean" -> java.lang.Boolean::class
        "char" -> java.lang.Character::class
        "byte" -> java.lang.Byte::class
        "short" -> java.lang.Short::class
        "int" -> java.lang.Integer::class
        "float" -> java.lang.Float::class
        "long" -> java.lang.Long::class
        "double" -> java.lang.Double::class
        else -> type
    }
}