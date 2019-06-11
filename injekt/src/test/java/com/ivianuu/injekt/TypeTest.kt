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
import kotlin.reflect.KClass

class TypeTest {

    private enum class Primitive {
        BOOLEAN {
            override val nonNullReified: Type<*> = typeOf<Boolean>()
            override val nullableReified: Type<*> = typeOf<Boolean?>()
            override val unboxedClass: KClass<*> = Boolean::class
            override val boxedClass: KClass<*> = java.lang.Boolean::class
        },
        BYTE {
            override val nonNullReified: Type<*> = typeOf<Byte>()
            override val nullableReified: Type<*> = typeOf<Byte?>()
            override val unboxedClass: KClass<*> = Byte::class
            override val boxedClass: KClass<*> = java.lang.Byte::class
        },
        CHAR {
            override val nonNullReified: Type<*> = typeOf<Char>()
            override val nullableReified: Type<*> = typeOf<Char?>()
            override val unboxedClass: KClass<*> = Char::class
            override val boxedClass: KClass<*> = java.lang.Character::class
        },
        DOUBLE {
            override val nonNullReified: Type<*> = typeOf<Double>()
            override val nullableReified: Type<*> = typeOf<Double?>()
            override val unboxedClass: KClass<*> = Double::class
            override val boxedClass: KClass<*> = java.lang.Double::class
        },
        FLOAT {
            override val nonNullReified: Type<*> = typeOf<Float>()
            override val nullableReified: Type<*> = typeOf<Float?>()
            override val unboxedClass: KClass<*> = Float::class
            override val boxedClass: KClass<*> = java.lang.Float::class
        },
        INT {
            override val nonNullReified: Type<*> = typeOf<Int>()
            override val nullableReified: Type<*> = typeOf<Int?>()
            override val unboxedClass: KClass<*> = Int::class
            override val boxedClass: KClass<*> = java.lang.Integer::class
        },
        LONG {
            override val nonNullReified: Type<*> = typeOf<Long>()
            override val nullableReified: Type<*> = typeOf<Long?>()
            override val unboxedClass: KClass<*> = Long::class
            override val boxedClass: KClass<*> = java.lang.Long::class
        },
        SHORT {
            override val nonNullReified: Type<*> = typeOf<Short>()
            override val nullableReified: Type<*> = typeOf<Short?>()
            override val unboxedClass: KClass<*> = Short::class
            override val boxedClass: KClass<*> = java.lang.Short::class
        };

        abstract val nonNullReified: Type<*>
        abstract val nullableReified: Type<*>
        abstract val unboxedClass: KClass<*>
        abstract val boxedClass: KClass<*>
    }

    @Test
    fun testPrimitiveMapping() {
        Primitive.values().forEach { type ->
            val nonNullReified = type.nonNullReified
            val nullableReified = type.nullableReified

            val nonNullUnboxedClass = typeOf<Any?>(type.unboxedClass)
            val nullableUnboxedClass = typeOf<Any?>(type.unboxedClass, true)

            val nonNullBoxedClass = typeOf<Any?>(type.boxedClass)
            val nullableBoxedClass = typeOf<Any?>(type.boxedClass, true)

            val nonNullUnboxedJavaType = typeOf<Any?>(type.unboxedClass.java)
            val nullableUnboxedJavaType = typeOf<Any?>(type.unboxedClass.java, true)

            val nonNullBoxedJavaType = typeOf<Any?>(type.boxedClass.java)
            val nullableBoxedJavaType = typeOf<Any?>(type.boxedClass.java, true)

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