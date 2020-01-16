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

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import kotlin.reflect.KClass
import org.junit.Test

class TypeTest {

    private enum class Primitive(
        val primitiveType: KClass<*>,
        val nonNullReified: Type<*>,
        val nullableReified: Type<*>
    ) {
        Boolean(kotlin.Boolean::class, typeOf<kotlin.Boolean>(), typeOf<kotlin.Boolean?>()),
        Byte(kotlin.Byte::class, typeOf<kotlin.Byte>(), typeOf<kotlin.Byte?>()),
        Char(kotlin.Char::class, typeOf<kotlin.Char>(), typeOf<kotlin.Char?>()),
        Double(kotlin.Double::class, typeOf<kotlin.Double>(), typeOf<kotlin.Double?>()),
        Float(kotlin.Float::class, typeOf<kotlin.Float>(), typeOf<kotlin.Float?>()),
        Int(kotlin.Int::class, typeOf<kotlin.Int>(), typeOf<kotlin.Int?>()),
        Long(kotlin.Long::class, typeOf<kotlin.Long>(), typeOf<kotlin.Long?>()),
        Short(kotlin.Short::class, typeOf<kotlin.Short>(), typeOf<kotlin.Short?>());
    }

    @Test
    fun testPrimitiveMapping() {
        Primitive.values().forEach { type ->
            val nonNullReified = type.nonNullReified
            val nullableReified = type.nullableReified

            val nonNullPrimitive = typeOf<Any?>(type.primitiveType)
            val nullablePrimitive = typeOf<Any?>(type.primitiveType, true)

            val nonNullObject = typeOf<Any?>(type.primitiveType.javaObjectType)
            val nullableObject = typeOf<Any?>(type.primitiveType.javaObjectType, true)

            val nonNullPrimitiveJava = typeOf<Any?>(type.primitiveType.java)
            val nullablePrimitiveJava = typeOf<Any?>(type.primitiveType.java, true)

            val nonNullObjectJava = typeOf<Any?>(type.primitiveType.javaObjectType)
            val nullableObjectJava = typeOf<Any?>(type.primitiveType.javaObjectType, true)

            val pairs = listOf(
                nonNullReified to nullableReified,
                nonNullPrimitive to nullablePrimitive,
                nonNullObject to nullableObject,
                nonNullPrimitiveJava to nullablePrimitiveJava,
                nonNullObjectJava to nullableObjectJava
            )

            pairs.forEach { (nonNull, nullable) -> assertTrue(nonNull != nullable) }

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

    @Test
    fun testParameterDistinction() {
        val listOfInts = typeOf<List<Int>>()
        val listOfStrings = typeOf<List<String>>()
        assertFalse(listOfInts == listOfStrings)
    }

    @Test
    fun testNullableDistinction() {
        val stringType = typeOf<String>()
        val nullableStringType = typeOf<String?>()
        assertFalse(stringType == nullableStringType)
    }
}
