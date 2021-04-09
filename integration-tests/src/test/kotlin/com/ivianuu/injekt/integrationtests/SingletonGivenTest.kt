package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.irShouldContain
import com.ivianuu.injekt.test.irShouldNotContain
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import org.junit.Test

class SingletonGivenTest {
    @Test
    fun testSingletonGiven() = codegen(
        """
            @Given class MyModule {
                @Given fun foo() = Foo()
            }

            fun invoke() = given<Foo>()
        """
    ) {
        irShouldContain(1, "var INSTANCE: MyModule")
        invokeSingleFile()
    }

    @Test
    fun testSingletonGivenMulti() = multiCodegen(
        listOf(
            source(
                """
                    @Given class MyModule {
                        @Given fun foo() = Foo()
                    }
                """
            )
        ),
        listOf(
            source(
                """
                   fun invoke() = given<Foo>() 
                """,
                name = "File.kt"
            )
        )
    ) {
        it.invokeSingleFile()
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
