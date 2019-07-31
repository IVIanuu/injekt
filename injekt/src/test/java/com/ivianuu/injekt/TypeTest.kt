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

import junit.framework.Assert.*
import org.junit.Test
import kotlin.reflect.KClass

class TypeTest {

    private enum class Primitive(
        val primitiveType: KClass<*>,
        val nonNullReified: Type<*>,
        val nullableReified: Type<*>
    ) {
        BOOLEAN(Boolean::class, typeOf<Boolean>(), typeOf<Boolean?>()),
        BYTE(Byte::class, typeOf<Byte>(), typeOf<Byte?>()),
        CHAR(Char::class, typeOf<Char>(), typeOf<Char?>()),
        DOUBLE(Double::class, typeOf<Double>(), typeOf<Double?>()),
        FLOAT(Float::class, typeOf<Float>(), typeOf<Float?>()),
        INT(Int::class, typeOf<Int>(), typeOf<Int?>()),
        LONG(Long::class, typeOf<Long>(), typeOf<Long?>()),
        SHORT(Short::class, typeOf<Short>(), typeOf<Short?>());
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

            for (i in 0 until nonNulls.size) {
                for (j in i + 1 until nonNulls.size) {
                    val a = nonNulls[i]
                    val b = nonNulls[j]
                    assertEquals(a, b)
                }
            }

            val nulls = pairs.map { it.second }

            for (i in 0 until nulls.size) {
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