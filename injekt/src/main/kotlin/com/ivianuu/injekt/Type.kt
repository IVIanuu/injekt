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
 * Describes a injectable type [Type]
 */
data class Type<T> internal constructor(
    val raw: KClass<*>,
    val isNullable: Boolean,
    val parameters: Array<out Type<*>>
) {

    val rawJava = raw.java

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Type<*>) return false

        if (rawJava != other.rawJava) return false
        if (!parameters.contentEquals(other.parameters)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rawJava.hashCode()
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

@PublishedApi
internal fun <T> KType.asType(): Type<T> {
    val parameters = arrayOfNulls<Type<*>>(arguments.size)
    arguments.forEachIndexed { i, kType ->
        parameters[i] = kType.type?.asType<Any?>() ?: typeOf<Any?>()
    }

    return Type(
        (classifier ?: Any::class) as KClass<*>,
        isMarkedNullable,
        parameters as Array<out Type<*>>
    )
}

fun <T> typeOf(
    raw: KClass<*>,
    vararg parameters: Type<*>,
    isNullable: Boolean = false
): Type<T> {
    val finalRaw = if (isNullable) boxed(raw) else unboxed(raw)
    return Type(raw = finalRaw, isNullable = isNullable, parameters = parameters)
}

fun <T> typeOf(instance: T): Type<T> = typeOf((instance as Any)::class)

fun <T> typeOf(type: java.lang.reflect.Type, isNullable: Boolean = false): Type<T> {
    if (type is WildcardType) {
        if (type.upperBounds.isNotEmpty()) {
            return typeOf(type.upperBounds.first(), isNullable)
        } else if (type.lowerBounds.isNotEmpty()) {
            return typeOf(type.lowerBounds.first(), isNullable)
        }
    }

    if (type is ParameterizedType) {
        return Type(
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
