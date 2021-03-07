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

import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.assertMessage
import com.ivianuu.injekt.test.assertNoMessage
import com.ivianuu.injekt.test.codegen
import org.junit.Test

class GivenDeclarationCheckTest {

    @Test
    fun testClassWithGivenAnnotationAndGivenConstructor() = codegen(
        """
            @Given class Dep @Given constructor()
        """
    ) {
        assertCompileError("class cannot be marked with @Given if it has a @Given marked constructor")
    }

    @Test
    fun test() = codegen(
        """
            @Scoped<AppComponent>
            @Given
            class Dep(@Given app: App)

            @Scoped<AppComponent>
            @Given
            class DepWrapper(@Given dep: Dep)

            fun invoke() {
                "".initializeApp()   
            }
            @ComponentElementBinding<AppComponent>
            @Given
            class MyComponent(@Given dep: Dep, @Given wrapper: DepWrapper)

            //@ComponentInitializerBinding
            //@Given
            //fun myInitializer(@Given dep: Dep, @Given wrapper: DepWrapper): ComponentInitializer<AppComponent> = {}
        """
    )

    @Test
    fun testClassWithMultipleGivenConstructors() = codegen(
        """
            class Dep {
                @Given constructor(@Given foo: Foo)
                @Given constructor(@Given bar: Bar)
            }
        """
    ) {
        assertCompileError("class cannot have multiple @Given marked constructors")
    }

    @Test
    fun testClassWithGivenSupertypeWithoutGiven() = codegen(
        """
            interface Dep

            class DepImpl : @Given Dep
        """
    ) {
        assertCompileError("class with a @Given super type must be marked with @Given or must have a @Given marked constructor")
    }

    @Test
    fun testNonGivenValueParameterOnGivenFunction() = codegen(
        """
            @Given fun bar(foo: Foo) = Bar(foo)
        """
    ) {
        assertCompileError("Non @Given parameter")
    }

    @Test
    fun testNonGivenValueParameterOnGivenClass() = codegen(
        """
            @Given class MyBar(foo: Foo)
        """
    ) {
        assertCompileError("Non @Given parameter")
    }

    @Test
    fun testGivenLambdaWithNonGivenParameter() = codegen(
            """
            val lambda: @Given (Foo) -> Bar = { Bar(it) }
        """
    ) {
        assertCompileError("Non @Given parameter")
    }

    @Test
    fun testUsedGivenParameterIsNotMarkedAsUnused() = codegen(
        """
            fun func1(@Given foo: Foo) {
                func2()                
            }

            fun func2(@Given foo: Foo) {
                foo
            }
        """
    ) {
        assertNoMessage("Parameter 'foo' is never used")
    }

    @Test
    fun testUnusedGivenParameterIsMarkedAsUnused() = codegen(
        """
            fun func1(@Given foo: Foo) {
            }

            fun func2(@Given foo: Foo) {
                foo
            } 
        """
    ) {
        assertMessage("Parameter 'foo' is never used")
    }

}
