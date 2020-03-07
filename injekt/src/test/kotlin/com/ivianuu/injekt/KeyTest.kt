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

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import org.junit.Test
import kotlin.reflect.KClass

class KeyTest {

    @Test
    fun testArgumentDistinction() {
        val listOfInts = keyOf<List<Int>>()
        val listOfStrings = keyOf<List<String>>()
        assertFalse(listOfInts == listOfStrings)
    }

    // todo not supported yet @Test
    fun testAnnotationDistinction() {
        val typeA = keyOf<@TypeAnnotationOne String>()
        val typeB = keyOf<@TypeAnnotationTwo String>()
        assertFalse(typeA == typeB)
    }

    // todo not supported for now @Test
    fun testNullableDistinction() {
        val nonNull = keyOf<String>()
        val nullable = keyOf<String?>()
        assertFalse(nonNull == nullable)
    }

    private enum class Primitive(
        val primitiveClassifier: KClass<*>,
        val nonNullReified: Key<*>,
        val nullableReified: Key<*>
    ) {
        Boolean(kotlin.Boolean::class, keyOf<kotlin.Boolean>(), keyOf<kotlin.Boolean?>()),
        Byte(kotlin.Byte::class, keyOf<kotlin.Byte>(), keyOf<kotlin.Byte?>()),
        Char(kotlin.Char::class, keyOf<kotlin.Char>(), keyOf<kotlin.Char?>()),
        Double(kotlin.Double::class, keyOf<kotlin.Double>(), keyOf<kotlin.Double?>()),
        Float(kotlin.Float::class, keyOf<kotlin.Float>(), keyOf<kotlin.Float?>()),
        Int(kotlin.Int::class, keyOf<kotlin.Int>(), keyOf<kotlin.Int?>()),
        Long(kotlin.Long::class, keyOf<kotlin.Long>(), keyOf<kotlin.Long?>()),
        Short(kotlin.Short::class, keyOf<kotlin.Short>(), keyOf<kotlin.Short?>());
    }

    @Test
    fun testPrimitiveMapping() {
        Primitive.values().forEach { type ->
            val nonNullReified = type.nonNullReified
            val nullableReified = type.nullableReified

            val nonNullPrimitive = keyOf<Any?>(type.primitiveClassifier)
            val nullablePrimitive = keyOf<Any?>(type.primitiveClassifier, isNullable = true)

            val nonNullObject = keyOf<Any?>(type.primitiveClassifier.javaObjectType.kotlin)
            val nullableObject =
                keyOf<Any?>(type.primitiveClassifier.javaObjectType.kotlin, isNullable = true)

            val nonNullPrimitiveJava = keyOf<Any?>(type.primitiveClassifier)
            val nullablePrimitiveJava = keyOf<Any?>(type.primitiveClassifier, isNullable = true)

            val nonNullObjectJava = keyOf<Any?>(type.primitiveClassifier.javaObjectType.kotlin)
            val nullableObjectJava =
                keyOf<Any?>(type.primitiveClassifier.javaObjectType.kotlin, isNullable = true)

            val pairs = listOf(
                nonNullReified to nullableReified,
                nonNullPrimitive to nullablePrimitive,
                nonNullObject to nullableObject,
                nonNullPrimitiveJava to nullablePrimitiveJava,
                nonNullObjectJava to nullableObjectJava
            )

            // todo not supported for now pairs.forEach { (nonNull, nullable) -> assertTrue(nonNull != nullable) }

            val nonNulls = pairs.map { it.first }

            for (i in nonNulls.indices) {
                for (j in i + 1 until nonNulls.size) {
                    val a = nonNulls[i]
                    val b = nonNulls[j]
                    assertEquals(a, b)
                }
            }

            val nulls = pairs.map { it.second }

            for (i in nulls.indices) {
                for (j in i + 1 until nulls.size) {
                    val a = nulls[i]
                    val b = nulls[j]
                    assertEquals(a, b)
                }
            }
        }
    }
}


private annotation class TypeAnnotationOne

private annotation class TypeAnnotationTwo