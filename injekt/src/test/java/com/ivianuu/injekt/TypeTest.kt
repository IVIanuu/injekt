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

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

class TypeTest {

    private enum class Primitive {
        BOOLEAN {
            override val nonNullReified: Type<*> = typeOf<Boolean>()
            override val nullableReified: Type<*> = typeOf<Boolean?>()
            override val nonNullUnboxedClass: Type<*> = typeOf<Boolean>(Boolean::class)
            override val nullableUnboxedClass: Type<*> = typeOf<Boolean?>(Boolean::class, true)
            override val nonNullBoxedClass: Type<*> = typeOf<Boolean>(java.lang.Boolean::class)
            override val nullableBoxedClass: Type<*> =
                typeOf<Boolean?>(java.lang.Boolean::class, true)
            override val nonNullUnboxedJavaType: Type<*> = typeOf<Boolean>(Boolean::class.java)
            override val nullableUnboxedJavaType: Type<*> =
                typeOf<Boolean?>(Boolean::class.java, true)
            override val nonNullBoxedJavaType: Type<*> =
                typeOf<Boolean>(java.lang.Boolean::class.java)
            override val nullableBoxedJavaType: Type<*> =
                typeOf<Boolean?>(java.lang.Boolean::class.java, true)
        },
        BYTE {
            override val nonNullReified: Type<*> = typeOf<Byte>()
            override val nullableReified: Type<*> = typeOf<Byte?>()
            override val nonNullUnboxedClass: Type<*> = typeOf<Byte>(Byte::class)
            override val nullableUnboxedClass: Type<*> = typeOf<Byte?>(Byte::class, true)
            override val nonNullBoxedClass: Type<*> = typeOf<Byte>(java.lang.Byte::class)
            override val nullableBoxedClass: Type<*> = typeOf<Byte?>(java.lang.Byte::class, true)
            override val nonNullUnboxedJavaType: Type<*> = typeOf<Byte>(Byte::class.java)
            override val nullableUnboxedJavaType: Type<*> = typeOf<Byte?>(Byte::class.java, true)
            override val nonNullBoxedJavaType: Type<*> = typeOf<Byte>(java.lang.Byte::class.java)
            override val nullableBoxedJavaType: Type<*> =
                typeOf<Byte?>(java.lang.Byte::class.java, true)
        },
        CHAR {
            override val nonNullReified: Type<*> = typeOf<Char>()
            override val nullableReified: Type<*> = typeOf<Char?>()
            override val nonNullUnboxedClass: Type<*> = typeOf<Char>(Char::class)
            override val nullableUnboxedClass: Type<*> = typeOf<Char?>(Char::class, true)
            override val nonNullBoxedClass: Type<*> = typeOf<Char>(java.lang.Character::class)
            override val nullableBoxedClass: Type<*> =
                typeOf<Char?>(java.lang.Character::class, true)
            override val nonNullUnboxedJavaType: Type<*> = typeOf<Char>(Char::class.java)
            override val nullableUnboxedJavaType: Type<*> = typeOf<Char?>(Char::class.java, true)
            override val nonNullBoxedJavaType: Type<*> =
                typeOf<Char>(java.lang.Character::class.java)
            override val nullableBoxedJavaType: Type<*> =
                typeOf<Char?>(java.lang.Character::class.java, true)
        },
        DOUBLE {
            override val nonNullReified: Type<*> = typeOf<Double>()
            override val nullableReified: Type<*> = typeOf<Double?>()
            override val nonNullUnboxedClass: Type<*> = typeOf<Double>(Double::class)
            override val nullableUnboxedClass: Type<*> = typeOf<Double?>(Double::class, true)
            override val nonNullBoxedClass: Type<*> = typeOf<Double>(java.lang.Double::class)
            override val nullableBoxedClass: Type<*> =
                typeOf<Double?>(java.lang.Double::class, true)
            override val nonNullUnboxedJavaType: Type<*> = typeOf<Double>(Double::class.java)
            override val nullableUnboxedJavaType: Type<*> =
                typeOf<Double?>(Double::class.java, true)
            override val nonNullBoxedJavaType: Type<*> =
                typeOf<Double>(java.lang.Double::class.java)
            override val nullableBoxedJavaType: Type<*> =
                typeOf<Double?>(java.lang.Double::class.java, true)
        },
        FLOAT {
            override val nonNullReified: Type<*> = typeOf<Float>()
            override val nullableReified: Type<*> = typeOf<Float?>()
            override val nonNullUnboxedClass: Type<*> = typeOf<Float>(Float::class)
            override val nullableUnboxedClass: Type<*> = typeOf<Float?>(Float::class, true)
            override val nonNullBoxedClass: Type<*> = typeOf<Float>(java.lang.Float::class)
            override val nullableBoxedClass: Type<*> = typeOf<Float?>(java.lang.Float::class, true)
            override val nonNullUnboxedJavaType: Type<*> = typeOf<Float>(Float::class.java)
            override val nullableUnboxedJavaType: Type<*> = typeOf<Float?>(Float::class.java, true)
            override val nonNullBoxedJavaType: Type<*> = typeOf<Float>(java.lang.Float::class.java)
            override val nullableBoxedJavaType: Type<*> =
                typeOf<Float?>(java.lang.Float::class.java, true)
        },
        INT {
            override val nonNullReified: Type<*> = typeOf<Int>()
            override val nullableReified: Type<*> = typeOf<Int?>()
            override val nonNullUnboxedClass: Type<*> = typeOf<Int>(Int::class)
            override val nullableUnboxedClass: Type<*> = typeOf<Int?>(Int::class, true)
            override val nonNullBoxedClass: Type<*> = typeOf<Int>(java.lang.Integer::class)
            override val nullableBoxedClass: Type<*> = typeOf<Int?>(java.lang.Integer::class, true)
            override val nonNullUnboxedJavaType: Type<*> = typeOf<Int>(Int::class.java)
            override val nullableUnboxedJavaType: Type<*> = typeOf<Int?>(Int::class.java, true)
            override val nonNullBoxedJavaType: Type<*> = typeOf<Int>(java.lang.Integer::class.java)
            override val nullableBoxedJavaType: Type<*> =
                typeOf<Int?>(java.lang.Integer::class.java, true)
        },
        LONG {
            override val nonNullReified: Type<*> = typeOf<Long>()
            override val nullableReified: Type<*> = typeOf<Long?>()
            override val nonNullUnboxedClass: Type<*> = typeOf<Long>(Long::class)
            override val nullableUnboxedClass: Type<*> = typeOf<Long?>(Long::class, true)
            override val nonNullBoxedClass: Type<*> = typeOf<Long>(java.lang.Long::class)
            override val nullableBoxedClass: Type<*> = typeOf<Long?>(java.lang.Long::class, true)
            override val nonNullUnboxedJavaType: Type<*> = typeOf<Long>(Long::class.java)
            override val nullableUnboxedJavaType: Type<*> = typeOf<Long?>(Long::class.java, true)
            override val nonNullBoxedJavaType: Type<*> = typeOf<Long>(java.lang.Long::class.java)
            override val nullableBoxedJavaType: Type<*> =
                typeOf<Long?>(java.lang.Long::class.java, true)
        },
        SHORT {
            override val nonNullReified: Type<*> = typeOf<Short>()
            override val nullableReified: Type<*> = typeOf<Short?>()
            override val nonNullUnboxedClass: Type<*> = typeOf<Short>(Short::class)
            override val nullableUnboxedClass: Type<*> = typeOf<Short?>(Short::class, true)
            override val nonNullBoxedClass: Type<*> = typeOf<Short>(java.lang.Short::class)
            override val nullableBoxedClass: Type<*> = typeOf<Short?>(java.lang.Short::class, true)
            override val nonNullUnboxedJavaType: Type<*> = typeOf<Short>(Short::class.java)
            override val nullableUnboxedJavaType: Type<*> = typeOf<Short?>(Short::class.java, true)
            override val nonNullBoxedJavaType: Type<*> = typeOf<Short>(java.lang.Short::class.java)
            override val nullableBoxedJavaType: Type<*> =
                typeOf<Short?>(java.lang.Short::class.java, true)
        };

        abstract val nonNullReified: Type<*>
        abstract val nullableReified: Type<*>
        abstract val nonNullUnboxedClass: Type<*>
        abstract val nullableUnboxedClass: Type<*>
        abstract val nonNullBoxedClass: Type<*>
        abstract val nullableBoxedClass: Type<*>
        abstract val nonNullUnboxedJavaType: Type<*>
        abstract val nullableUnboxedJavaType: Type<*>
        abstract val nonNullBoxedJavaType: Type<*>
        abstract val nullableBoxedJavaType: Type<*>
    }

    @Test
    fun testPrimitiveMapping() {
        Primitive.values().forEach { type ->
            val nonNullReified = type.nonNullReified
            val nullableReified = type.nullableReified

            val nonNullUnboxedClass = type.nonNullUnboxedClass
            val nullableUnboxedClass = type.nullableUnboxedClass

            val nonNullBoxedClass = type.nonNullBoxedClass
            val nullableBoxedClass = type.nullableBoxedClass

            val nonNullUnboxedJavaType = type.nonNullUnboxedJavaType
            val nullableUnboxedJavaType = type.nullableUnboxedJavaType

            val nonNullBoxedJavaType = type.nonNullBoxedJavaType
            val nullableBoxedJavaType = type.nullableBoxedJavaType

            val pairs = listOf(
                nonNullReified to nullableReified,
                nonNullUnboxedClass to nullableUnboxedClass,
                nonNullBoxedClass to nullableBoxedClass,
                nonNullUnboxedJavaType to nullableUnboxedJavaType,
                nonNullBoxedJavaType to nullableBoxedJavaType
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