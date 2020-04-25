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

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Identifier for [Binding]s
 */
sealed class Key<T>(
    val classifier: KClass<*>,
    val qualifier: KClass<*>? = null,
    val isNullable: Boolean
) {

    fun copy(
        classifier: KClass<*> = this.classifier,
        qualifier: KClass<*>? = this.qualifier,
        isNullable: Boolean = this.isNullable,
        arguments: Array<Key<*>>? = (this as? ParameterizedKey)?.arguments
    ): Key<T> {
        return if (arguments != null) ParameterizedKey(classifier, qualifier, isNullable, arguments)
        else SimpleKey(classifier, qualifier, isNullable)
    }

    class SimpleKey<T> : Key<T> {

        private val hashCode: Int

        constructor(
            classifier: KClass<*>,
            qualifier: KClass<*>?,
            isNullable: Boolean
        ) : super(classifier, qualifier, isNullable) {
            this.hashCode = generateHashCode()
        }

        constructor(
            classifier: KClass<*>,
            qualifier: KClass<*>?,
            isNullable: Boolean,
            hashCode: Int
        ) : super(classifier, qualifier, isNullable) {
            this.hashCode = hashCode
        }

        override fun hashCode(): Int = hashCode

        override fun equals(other: Any?): Boolean =
            other is SimpleKey<*> && hashCode == other.hashCode

        private fun generateHashCode(): Int {
            var result = classifier.java.name.hashCode()
            // todo result = 31 * result + isNullable.hashCode()
            result = 31 * result + qualifier.hashCode()
            return result
        }

        override fun toString(): String {
            return qualifier.toString() +
                    "${classifier.java.name}${if (isNullable) "?" else ""})"
        }

    }

    class ParameterizedKey<T> : Key<T> {

        val arguments: Array<Key<*>>

        private val hashCode: Int

        constructor(
            classifier: KClass<*>,
            qualifier: KClass<*>?,
            isNullable: Boolean,
            arguments: Array<Key<*>>
        ) : super(classifier, qualifier, isNullable) {
            this.arguments = arguments
            this.hashCode = generateHashCode()
        }

        constructor(
            classifier: KClass<*>,
            qualifier: KClass<*>?,
            isNullable: Boolean,
            hashCode: Int,
            arguments: Array<Key<*>>
        ) : super(classifier, qualifier, isNullable) {
            this.arguments = arguments
            this.hashCode = hashCode
        }

        override fun hashCode(): Int = hashCode

        override fun equals(other: Any?): Boolean =
            other is ParameterizedKey<*> && hashCode == other.hashCode

        private fun generateHashCode(): Int {
            var result = classifier.java.name.hashCode()
            // todo result = 31 * result + isNullable.hashCode()
            result = 31 * result + arguments.contentHashCode()
            result = 31 * result + qualifier.hashCode()
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
            return qualifier.toString().let { "$it " } +
                    "${classifier.java.name}$params${if (isNullable) "?" else ""}"
        }

    }

}

inline fun <reified T> keyOf(qualifier: KClass<*>? = null): Key<T> =
    typeOf<T>().asKey(qualifier = qualifier)

fun <T> keyOf(
    classifier: KClass<*>,
    qualifier: KClass<*>? = null,
    isNullable: Boolean = false
): Key.SimpleKey<T> {
    return Key.SimpleKey(
        classifier = classifier,
        isNullable = isNullable,
        qualifier = qualifier
    )
}

fun <T> keyOf(
    classifier: KClass<*>,
    arguments: Array<Key<*>>,
    qualifier: KClass<*>? = null,
    isNullable: Boolean = false
): Key.ParameterizedKey<T> {
    return Key.ParameterizedKey(
        classifier = classifier,
        isNullable = isNullable,
        arguments = arguments,
        qualifier = qualifier
    )
}

@PublishedApi
internal fun <T> KType.asKey(qualifier: KClass<*>? = null): Key<T> {
    val classifier = (classifier ?: Any::class) as KClass<*>
    return if (arguments.isEmpty()) {
        keyOf(classifier, qualifier, isMarkedNullable)
    } else {
        val args = arrayOfNulls<Key<Any?>>(arguments.size)
        for (index in arguments.indices) {
            args[index] =
                arguments[index].type?.asKey() ?: keyOf(classifier = Any::class, isNullable = true)
        }
        keyOf(classifier, args as Array<Key<*>>, qualifier, isMarkedNullable)
    }
}
