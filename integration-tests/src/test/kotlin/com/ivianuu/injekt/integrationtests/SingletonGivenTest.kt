package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.irShouldNotContain
import com.ivianuu.injekt.test.singleAndMultiCodegen
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.Test

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
}