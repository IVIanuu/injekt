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
import com.ivianuu.injekt.test.invokeSingleFile
import org.junit.Test

class AbstractGivenTest {
    @Test
    fun testGivenInterface() = codegen(
        """
            @Given interface FooComponent {
                @Given val foo: Foo
            }

            @Given val foo = Foo()

            fun invoke() = given<FooComponent>().foo
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testAbstractGivenWithNonGivenMembers() = codegen(
        """
            @Given interface MyComponent {
                val foo: Foo
            }
        """
    ) {
        compilationShouldHaveFailed("abstract @Given can only have abstract @Given members. Implement the member yourself or remove it")
    }

    @Test
    fun testAbstractGivenWithVar() = codegen(
        """
            @Given interface MyComponent {
                @Given var foo: Foo
            }
        """
    ) {
        compilationShouldHaveFailed("abstract @Given cannot contain mutable properties")
    }

    @Test
    fun testInterfaceWithNonGivenMembersInSubInterface() = codegen(
        """
            interface MyBaseComponent {
                val foo: Foo
            }
            @Given interface MyComponent : MyBaseComponent
        """
    ) {
        compilationShouldHaveFailed("abstract @Given can only have abstract @Given members. Implement the member yourself or remove it")
    }

    @Test
    fun testGivenAbstractClass() = codegen(
        """
            @Given abstract class BarComponent(@Given protected val _foo: Foo) {
                @Given abstract val bar: Bar
            }

            @Given val foo = Foo()

            @Given fun bar(@Given foo: Foo) = Bar(foo)

            fun invoke() = given<BarComponent>().bar
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testGivenInterfaceFactoryFunction() = codegen(
        """
            @Given interface BarComponent {
                @Given fun bar(@Given foo: Foo): Bar
            }

            @Given fun bar(@Given foo: Foo) = Bar(foo)

            fun invoke() = given<BarComponent>().bar(Foo())
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testAbstractGivenWithUnexistingRequestButDefaultImplementationIsNoError() = codegen(
        """
            @Given interface BarComponent {
                @Given fun bar(@Given foo: Foo): Bar = Bar(foo)
            }

            @Given
            val foo = Foo()

            fun invoke() = given<BarComponent>().bar(Foo())
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testAbstractGivenWithErrorRequestButDefaultImplementationIsNoError() = codegen(
        """
            @Given interface BarComponent {
                @Given(useDefaultOnAllErrors = true)
                fun bar(): Bar = Bar(Foo())
            }

            @Given fun bar(@Given foo: Foo) = Bar(foo)

            fun invoke() = given<BarComponent>().bar()
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testGivenInterfaceWithTypeParameters() = codegen(
        """
            @Given interface ParameterizedComponent<T> {
                @Given val value: T
            }

            @Given val foo = Foo()

            fun invoke() = given<ParameterizedComponent<Foo>>().value
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testGivenInterfaceWithSuspendFunction() = codegen(
        """
            @Given interface FooComponent {
                @Given suspend fun foo(): Foo
            }

            @Given suspend fun foo() = Foo()

            fun invoke() = runBlocking { given<FooComponent>().foo() }
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testGivenInterfaceWithComposableProperty() = codegen(
        """
            @Given interface FooComponent {
                @Given @Composable fun foo(): Foo
            }

            @Given @Composable fun foo() = Foo()

            @Composable
            fun invoke() = given<FooComponent>().foo()
        """
    )

    @Test
    fun testGivenInterfaceWithImplementedSuperMember() = codegen(
        """
            @Given abstract class MyComponent : GivenScope by defaultGivenScope() {
                @Given abstract val foo: Foo
            }

            @Given val foo = Foo()

            fun invoke() = given<MyComponent>().foo
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testGivenAbstractClassWithProtectedGiven() = codegen(
        """
            @Given abstract class MyComponent {
                @Given protected abstract val protectedFoo: Foo
                val scopedFoo by lazy { protectedFoo }
            }

            @Given val foo = Foo()

            fun invoke() = given<MyComponent>().scopedFoo
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testGivenInterfaceIsCreatedOnTheFly() = codegen(
        """
            @Given interface MyComponent {
                @Given val foo: Foo
            }

            fun invoke() = given<(@Given Foo) -> MyComponent>()
        """
    ) {
        invokeSingleFile()
    }
}
