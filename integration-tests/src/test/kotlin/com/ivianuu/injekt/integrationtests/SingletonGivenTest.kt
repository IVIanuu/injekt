package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.*
import io.kotest.matchers.types.*
import org.junit.*

class SingletonGivenTest {
    @Test
    fun testSingletonGiven() = singleAndMultiCodegen(
        """
            @Given class MyModule {
                @Given fun foo() = Foo()
            }
        """,
        """
           fun invoke() = given<MyModule>()
        """
    ) {
        invokeSingleFile()
            .shouldBeSameInstanceAs(invokeSingleFile())
    }

    @Test
    fun testDoesNotOptimizeNormalClass() = codegen(
        """
            class MyModule
            @Given val foo = Foo()
            fun invoke() = given<Foo>()
        """
    ) {
        irShouldNotContain("var INSTANCE: MyModule")
    }

    @Test
    fun testDoesNotOptimizeObject() = codegen(
        """
            @Given object MyModule {
                @Given val foo = Foo()
            }
            fun invoke() = given<Foo>()
        """
    ) {
        irShouldNotContain("var INSTANCE: MyModule")
    }

    @Test
    fun testDoesNotOptimizeGivenWithConstructorParameters() = codegen(
        """
            @Given class MyModule(@Given val foo: Foo)
            @Given val foo = Foo()
            fun invoke() = given<Foo>()
        """
    ) {
        irShouldNotContain("var INSTANCE: MyModule")
    }

    @Test
    fun testDoesNotOptimizeGivenWithForTypeKeyParameters() = codegen(
        """
            @Given class MyModule<@ForTypeKey T> {
                @Given val instance = Foo() as T
            }
            fun invoke() = given<Foo>()
        """
    ) {
        irShouldNotContain("var INSTANCE: MyModule")
    }

    @Test
    fun testDoesNotOptimizeGivenWithFields() = codegen(
        """
            @Given class MyModule {
                @Given val foo = Foo()
            }
            fun invoke() = given<Foo>()
        """
    ) {
        irShouldNotContain("var INSTANCE: MyModule")
    }

    @Test
    fun testDoesNotOptimizeGivenWithInnerClass() = codegen(
        """
            @Given class MyModule {
                inner class Inner
            }
            @Given val foo = Foo()
            fun invoke() = given<Foo>()
        """
    ) {
        irShouldNotContain("var INSTANCE: MyModule")
    }

    @Test
    fun testDoesOptimizeGivenWithComputedProperties() = codegen(
        """
            @Given class MyModule {
                @Given val foo get() = Foo()
            }
            fun invoke() = given<Foo>()
        """
    ) {
        irShouldContain(1, "var INSTANCE: MyModule")
    }
}
