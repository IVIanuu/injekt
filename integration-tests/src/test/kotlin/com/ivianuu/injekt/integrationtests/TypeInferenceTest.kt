/*
 * Copyright 2021 Manuel Wrage
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

import com.ivianuu.injekt.common.*
import com.ivianuu.injekt.scope.*
import com.ivianuu.injekt.test.*
import org.junit.*

class TypeInferenceTest {
    @Test
    fun testFunctionExpressionInference() = singleAndMultiCodegen(
        """
            class ChildGivenScopeModule<P : GivenScope, T, S : T> {
                @Given
                fun factory(
                    @Given scopeFactory: S
                ): @InstallElement<P> @ChildScopeFactory T = scopeFactory
            }
            fun <P : GivenScope, P1, C : GivenScope> ChildGivenScopeModule1() = 
                ChildGivenScopeModule<P, 
                (P1) -> C, (@Given @InstallElement<C> P1) -> C>()

            typealias MyGivenScope = GivenScope
            """,
        """
            @Given
            fun myGivenScopeModule() = 
                ChildGivenScopeModule1<AppGivenScope, Foo, MyGivenScope>()

            @Given fun bar(@Given foo: Foo) = Bar(foo)

            @GivenImports("com.ivianuu.injekt.common.*", "com.ivianuu.injekt.scope.*")
            fun invoke() = given<AppGivenScope>()
                .element<@ChildScopeFactory (Foo) -> MyGivenScope>()
                .invoke(Foo())
                .element<Foo>()
                """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testPropertyExpressionInference() = singleAndMultiCodegen(
        """
            class ChildGivenScopeModule<P : GivenScope, T, S : T> {
                @Given
                fun factory(
                    @Given scopeFactory: S
                ): @InstallElement<P> @ChildScopeFactory T = scopeFactory
            }
            fun <P : GivenScope, P1, C : GivenScope> ChildGivenScopeModule1() = 
                ChildGivenScopeModule<P, 
                (P1) -> C, (@Given @InstallElement<C> P1) -> C>()

            typealias MyGivenScope = GivenScope
            """,
        """
            @Given
            val myGivenScopeModule = 
                ChildGivenScopeModule1<AppGivenScope, Foo, MyGivenScope>()

            @Given fun bar(@Given foo: Foo) = Bar(foo)

            @GivenImports("com.ivianuu.injekt.common.*", "com.ivianuu.injekt.scope.*")
            fun invoke() = given<AppGivenScope>()
                .element<@ChildScopeFactory (Foo) -> MyGivenScope>()
                .invoke(Foo())
                .element<Foo>()
                """
    ) {
        invokeSingleFile(GivenScope<AppGivenScope>(typeKey = TypeKey("AppGivenScope")))
    }
}