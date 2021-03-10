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

import com.ivianuu.injekt.common.TypeKey
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import io.kotest.matchers.shouldBe
import org.junit.Test

class TypeKeyTest {

    @Test
    fun testTypeKeyOf() = codegen(
        """
           fun invoke() = typeKeyOf<String>() 
        """
    ) {
        invokeSingleFile<TypeKey<String>>().value shouldBe "kotlin.String"
    }

    @Test
    fun testForTypeKeyTypeParameter() = codegen(
        """
            inline fun <@ForTypeKey T> listTypeKeyOf() = typeKeyOf<List<T>>()
            fun invoke() = listTypeKeyOf<String>() 
        """
    ) {
        invokeSingleFile<TypeKey<List<String>>>().value shouldBe "kotlin.collections.List<kotlin.String>"
    }

    @Test
    fun testForTypeKeyTypeParameterInInterface() = codegen(
        """
            interface KeyFactory {
                fun <@ForTypeKey T> listTypeKeyOf(): TypeKey<List<T>>
                companion object : KeyFactory {
                    override fun <@ForTypeKey T> listTypeKeyOf() = typeKeyOf<List<T>>()
                }
            }
            fun invoke() = KeyFactory.listTypeKeyOf<String>() 
        """
    ) {
        invokeSingleFile<TypeKey<List<String>>>().value shouldBe "kotlin.collections.List<kotlin.String>"
    }

    @Test
    fun testForTypeKeyTypeParameterMulti() = multiCodegen(
        listOf(
            source(
                """
                    inline fun <@ForTypeKey T> listTypeKeyOf() = typeKeyOf<List<T>>()
                """
            )
        ),
        listOf(
            source(
                """
                    fun invoke() = listTypeKeyOf<String>()
                """,
                name = "File.kt"
            )
        )
    ) {
        it.invokeSingleFile<TypeKey<List<String>>>().value shouldBe "kotlin.collections.List<kotlin.String>"
    }

    @Test
    fun testNonForTypeKeyTypeParameterCannotBeUsedForForTypeKey() = codegen(
        """
           fun <T> invoke() = typeKeyOf<T>() 
        """
    ) {
        compilationShouldHaveFailed()
    }

}