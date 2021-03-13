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
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.shouldContainMessage
import com.ivianuu.injekt.test.shouldNotContainMessage
import org.junit.Test

class GivenDeclarationCheckTest {

    @Test
    fun testClassWithGivenAnnotationAndGivenConstructor() = codegen(
        """
            @Given class Dep @Given constructor()
        """
    ) {
        compilationShouldHaveFailed("class cannot be marked with @Given if it has a @Given marked constructor")
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

            @Scoped<AppComponent>
            @Given
            class DepWrapper2(@Given dep: () -> Dep, @Given wrapper: () -> DepWrapper)

            fun invoke() {
                "".initializeApp()   
            }
            @ComponentElementBinding<AppComponent>
            @Given
            class MyComponent(@Given dep: Dep, @Given wrapper: () -> () -> DepWrapper, @Given wrapper2: () -> DepWrapper2)

            @Given
            fun myInitializer(@Given dep: Dep, @Given wrapper: () -> () -> DepWrapper, @Given wrapper2: () -> DepWrapper2): ComponentInitializer<AppComponent> = {}
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
        compilationShouldHaveFailed("class cannot have multiple @Given marked constructors")
    }

    @Test
    fun testGivenAnnotationClass() = codegen(
        """
            @Given annotation class MyAnnotation
        """
    ) {
        compilationShouldHaveFailed("annotation class cannot be marked with @Given")
    }

    @Test
    fun testGivenConstructorOnAnnotationClass() = codegen(
        """
            annotation class MyAnnotation @Given constructor()
        """
    ) {
        compilationShouldHaveFailed("annotation class constructor cannot be marked with @Given")
    }

    @Test
    fun testGivenTailrecFunction() = codegen(
        """
            tailrec fun factorial(n : Long, a : Long = 1) : Long {
                return if (n == 1L) a
                else factorial(n - 1, n * a)
            }
        """
    ) {
        compilationShouldHaveFailed("tailrec function cannot be marked with @Given")
    }

    @Test
    fun testGivenEnumClass() = codegen(
        """
            @Given enum class MyEnum
        """
    ) {
        compilationShouldHaveFailed("enum class cannot be marked with @Given")
    }

    @Test
    fun testGivenAbstractClass() = codegen(
        """
            @Given abstract class MyAbstractClass
        """
    ) {
        compilationShouldHaveFailed("abstract class cannot be marked with @Given")
    }

    @Test
    fun testNonGivenValueParameterOnGivenFunction() = codegen(
        """
            @Given fun bar(foo: Foo) = Bar(foo)
        """
    ) {
        compilationShouldHaveFailed("non @Given parameter")
    }

    @Test
    fun testNonGivenValueParameterOnGivenClass() = codegen(
        """
            @Given class MyBar(foo: Foo)
        """
    ) {
        compilationShouldHaveFailed("non @Given parameter")
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
        shouldNotContainMessage("Parameter 'foo' is never used")
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
        shouldContainMessage("Parameter 'foo' is never used")
    }

}
