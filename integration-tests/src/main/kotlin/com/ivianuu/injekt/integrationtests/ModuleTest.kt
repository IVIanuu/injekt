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

import com.ivianuu.injekt.test.codegen
import org.junit.Test

class ModuleTest {

    @Test
    fun testClassModule() = codegen(
        """
            @Given val foo = Foo()
            @Module class BarModule(@Given private val foo: Foo) {
                @Given val bar get() = Bar(foo)
            }
            fun invoke() = given<Bar>()
        """
    )

    @Test
    fun testObjectModule() = codegen(
        """
            @Given val foo = Foo()
            @Module object BarModule {
                @Given fun bar(@Given foo: Foo) = Bar(foo)
            }
            fun invoke() = given<Bar>()
        """
    )

    @Test fun testModuleLambdaParameter() = codegen(
        """
            class MyModule {
                @Given val foo = Foo()
            }

            @Given fun foo() = Foo()
            @Given fun bar(@Given foo: Foo) = Bar(foo)

            inline fun <R> withModule(
                block: (@Module MyModule) -> R
            ): R = block(MyModule())

            fun invoke() = withModule { 
                given<Bar>()
            }
        """
    )
}
