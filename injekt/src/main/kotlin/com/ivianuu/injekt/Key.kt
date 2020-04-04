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

import com.jakewharton.confundus.unsafeCast
import kotlin.jvm.internal.ClassBasedDeclarationContainer
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * A key used to retrieve [Binding]s in [Component]s
 *
 * @see Component.get
 * @see ComponentBuilder.bind
 */
data class Key<T> internal constructor(
    val classifier: KClass<*>,
    val isNullable: Boolean,
    val arguments: Array<Key<*>>,
    val qualifier: Qualifier
) {

    private val hashCode = generateHashCode()

    override fun hashCode(): Int = hashCode

    override fun equals(other: Any?): Boolean = other is Key<*> && hashCode == other.hashCode

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

        return "Key(type=${classifier.java.name}${if (isNullable) "?" else ""}$params, qualifier=$qualifier)"
    }

    private fun generateHashCode(): Int {
        var result = classifier.hashCode()
        // todo result = 31 * result + isNullable.hashCode()
        result = 31 * result + arguments.contentHashCode()
        result = 31 * result + qualifier.hashCode()
        return result
    }
}

inline fun <reified T> keyOf(qualifier: Qualifier = Qualifier.None): Key<T> =
    typeOf<T>().asKey(qualifier = qualifier)

fun <T> keyOf(
    classifier: KClass<*>,
    isNullable: Boolean = false,
    arguments: Array<Key<*>> = emptyArray(),
    qualifier: Qualifier = Qualifier.None
): Key<T> {
    // todo check for the type marker https://youtrack.jetbrains.com/issue/KT-34900
    val finalClassifier = if (isNullable) boxed(classifier) else unboxed(classifier)
    return Key(
        classifier = finalClassifier,
        isNullable = isNullable,
        arguments = arguments,
        qualifier = qualifier
    )
}

@PublishedApi
internal fun <T> KType.asKey(qualifier: Qualifier = Qualifier.None): Key<T> {
    val args = arrayOfNulls<Key<Any?>>(arguments.size)

    for (index in arguments.indices) {
        args[index] = arguments[index].type?.asKey() ?: keyOf(Any::class, isNullable = true)
    }

    return Key<T>(
        classifier = (classifier ?: Any::class).unsafeCast(),
        arguments = args.unsafeCast(),
        isNullable = isMarkedNullable,
        qualifier = qualifier
    ).also {
        Injekt.logger?.warn("keyOf intrinsic called for $it")
    }
}

@Target(AnnotationTarget.FUNCTION)
annotation class KeyOverload

@Target(AnnotationTarget.FUNCTION)
annotation class KeyOverloadStub

private fun unboxed(type: KClass<*>): KClass<*> {
    val jClass = type.unsafeCast<ClassBasedDeclarationContainer>().jClass
    if (jClass.isPrimitive) return type

    return when (jClass.name) {
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
    val jClass = type.unsafeCast<ClassBasedDeclarationContainer>().jClass
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
