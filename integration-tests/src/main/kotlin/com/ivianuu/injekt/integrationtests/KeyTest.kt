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

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.common.Key
import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert.assertEquals
import org.junit.Test

class KeyTest {

    @Test
    fun testKeyOf() = codegen(
        """
           fun invoke() = keyOf<String>() 
        """
    ) {
        assertEquals("kotlin.String",
            invokeSingleFile<Key<String>>().value)
    }

    @Test
    fun testForKeyTypeParameter() = codegen(
        """
            inline fun <@ForKey T> listKeyOf() = keyOf<List<T>>()
            fun invoke() = listKeyOf<String>() 
        """
    ) {
        assertEquals("kotlin.collections.List<kotlin.String>",
            invokeSingleFile<Key<List<String>>>().value)
    }

    @Test
    fun testForKeyTypeParameterInInterface() = codegen(
        """
            interface KeyFactory {
                fun <@ForKey T> listKeyOf(): Key<List<T>>
                companion object : KeyFactory {
                    override fun <@ForKey T> listKeyOf() = keyOf<List<T>>()
                }
            }
            fun invoke() = KeyFactory.listKeyOf<String>() 
        """
    ) {
        assertEquals("kotlin.collections.List<kotlin.String>",
            invokeSingleFile<Key<List<String>>>().value)
    }

    @Test
    fun testForKeyTypeParameterMulti() = multiCodegen(
        listOf(
            source(
                """
                    inline fun <@ForKey T> listKeyOf() = keyOf<List<T>>()
                """
            )
        ),
        listOf(
            source(
                """
                    fun invoke() = listKeyOf<String>()
                """,
                name = "File.kt"
            )
        )
    ) {
        assertEquals("kotlin.collections.List<kotlin.String>",
            it.last().invokeSingleFile<Key<List<String>>>().value
        )
    }

    @Test
    fun testNonForKeyTypeParameterCannotBeUsedForForKey() = codegen(
        """
           fun <T> invoke() = keyOf<T>() 
        """
    ) {
        assertCompileError()
    }

}