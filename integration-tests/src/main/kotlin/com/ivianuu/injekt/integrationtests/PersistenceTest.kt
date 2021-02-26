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

import com.ivianuu.injekt.test.assertMessage
import com.ivianuu.injekt.test.assertNoMessage
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert.assertEquals
import org.junit.Test

class PersistenceTest {

    @Test
    fun testPrimaryConstructorWithTypeAliasDependencyMulti() = multiCodegen(
        listOf(
            source(
                """
                    @Given class Dep(@Given value: MyAlias)
                    typealias MyAlias = String
                    @Given val myAlias: MyAlias = ""
                """
            )
        ),
        listOf(
            source(
                """
                    fun invoke() = given<Dep>()
                """
            )
        )
    )

    @Test
    fun testSecondaryConstructorWithTypeAliasDependencyMulti() = multiCodegen(
        listOf(
            source(
                """
                    class Dep {
                        @Given constructor(@Given value: MyAlias)
                    }
                    typealias MyAlias = String
                    @Given val myAlias: MyAlias = ""
                """
            )
        ),
        listOf(
            source(
                """
                    fun invoke() = given<Dep>()
                """
            )
        )
    )

    @Test
    fun testModuleDispatchReceiverTypeInference() = codegen(
        """
            class MyModule<T : S, S> {
                @Given fun provide(@Given value: S): T = value as T
            }

            @Module val module = MyModule<String, CharSequence>()

            @Given val value: CharSequence = "42"

            fun invoke() = given<String>()
        """
    ) {
        assertEquals("42", invokeSingleFile())
    }

    @Test
    fun testModuleDispatchReceiverTypeInferenceMulti() = multiCodegen(
        listOf(
            source(
                """
                    class MyModule<T : S, S> {
                        @Given fun provide(@Given value: S): T = value as T
                    }
        
                    @Module val module = MyModule<String, CharSequence>()
        
                    @Given val value: CharSequence = "42" 
                """
            )
        ),
        listOf(
            source(
                """
                   fun invoke() = given<String>() 
                """,
                name = "File.kt"
            )
        )
    ) {
        assertEquals("42", it.last().invokeSingleFile())
    }

    @Test
    fun testFunctionTypeParameterClassifier() = multiCodegen(
        listOf(
            source(
                """
                    var callCount = 0
                    @Qualifier annotation class MyQualifier
                    @Macro @Given fun <T : FuncA> impl(@Given instance: T): @MyQualifier T {
                        return instance
                    }

                    typealias FuncA = suspend () -> Unit
                    typealias FuncB = suspend () -> Boolean

                    @Given fun funcA(@Given funcB: FuncB): FuncA = { }
                    @Given fun funcB(): FuncB = { true }
                """
            )
        ),
        listOf(
            source(
                """
                    fun invoke() {
                        given<@MyQualifier FuncA>()
                        given<@MyQualifier FuncB>()
                    } 
                """
            )
        )
    ) {
        with(it.last()) {
            assertNoMessage("no given argument found of type @com_ivianuu_injekt_integrationtests_MyQualifier() com.ivianuu.injekt.integrationtests.FuncA for parameter value of function com.ivianuu.injekt.given")
            assertMessage("no given argument found of type @com_ivianuu_injekt_integrationtests_MyQualifier() com.ivianuu.injekt.integrationtests.FuncB for parameter value of function com.ivianuu.injekt.given")
        }
    }

}
