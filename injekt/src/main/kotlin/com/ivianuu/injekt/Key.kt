/*
 * Copyright 2020 Manuel Wrage
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

import kotlin.jvm.internal.ClassBasedDeclarationContainer
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * A key used to retrieve [Binding]s in [Component]s
 *
 * @see Component.get
 * @see ComponentBuilder.add
 */
data class Key<T> internal constructor(
    val classifier: KClass<*>,
    val isNullable: Boolean,
    val arguments: Array<Key<*>>,
    val name: Any?
) {

    private val hashCode = generateHashCode()

    override fun hashCode(): Int = hashCode

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Key<*>

        if (classifier != other.classifier) return false
        // todo if (isNullable && !other.isNullable) return false
        if (!arguments.contentEquals(other.arguments)) return false
        if (name != other.name) return false

        return true
    }

    private fun generateHashCode(): Int {
        var result = classifier.hashCode()
        // todo result = 31 * result + isNullable.hashCode()
        result = 31 * result + arguments.contentHashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        val params = if (arguments.isNotEmpty()) {
            arguments.joinToString(
                separator = ", ",
                prefix = "<",
                postfix = ">"
            ) { it.toString() }
        } else {
            ""
        }

        return "Key(type=${classifier.java.name}${if (isNullable) "?" else ""}$params, name=$name)"
    }
}

inline fun <reified T> keyOf(name: Any? = null): Key<T> = typeOf<T>().asKey(name = name)

fun <T> keyOf(
    classifier: KClass<*>,
    isNullable: Boolean = false,
    arguments: Array<Key<*>> = emptyArray(),
    name: Any? = null
): Key<T> {
    // todo check for the type marker https://youtrack.jetbrains.com/issue/KT-34900
    val finalClassifier = if (isNullable) boxed(classifier) else unboxed(classifier)
    return Key(
        classifier = finalClassifier,
        isNullable = isNullable,
        arguments = arguments,
        name = name
    )
}

@PublishedApi
internal fun <T> KType.asKey(name: Any? = null): Key<T> {
    val args = arrayOfNulls<Key<Any?>>(arguments.size)

    arguments.forEachIndexed { index, kTypeProjection ->
        args[index] = kTypeProjection.type?.asKey() ?: keyOf()
    }

    return Key(
        classifier = (classifier ?: Any::class) as KClass<*>,
        arguments = args as Array<Key<*>>,
        isNullable = isMarkedNullable,
        name = name
    )
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
