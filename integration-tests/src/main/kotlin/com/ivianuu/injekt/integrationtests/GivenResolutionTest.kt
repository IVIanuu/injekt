package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Bar
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
import org.junit.Test

class GivenResolutionTest {

    @Test
    fun testResolvesInternalGiven() = codegen(
        """
            @Given val foo = Foo()
            fun invoke() = given<Foo>()
        """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testResolvesExternalGiven() = multiCodegen(
        listOf(
            source(
                """
                    @Given val foo = Foo()
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
        assertTrue(it.last().invokeSingleFile() is Foo)
    }

    @Test
    fun testPrefersInternalGivenOverExternal() = multiCodegen(
        listOf(
            source(
                """
                    @Given lateinit var externalFoo: Foo
                """
            )
        ),
        listOf(
            source(
                """
                    @Given lateinit var internalFoo: Foo

                    fun invoke(internal: Foo, external: Foo): Foo {
                        externalFoo = external
                        internalFoo = internal
                        return given<Foo>()
                    }
                """,
                name = "File.kt"
            )
        )
    ) {
        val internal = Foo()
        val external = Foo()
        val result = it.last().invokeSingleFile(internal, external)
        assertSame(result, internal)
    }

    @Test
    fun testDerivedGiven() = codegen(
        """
            @Given val foo = Foo()
            @Given val bar = Bar(given())
            fun invoke() = given<Bar>()
        """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testCannotResolveExternalInternalMarkedGiven() = multiCodegen(
        listOf(
            source(
                """
                    @Given internal val foo = Foo()
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
        it.last().assertCompileError("Unresolved given for Foo")
    }

    @Test
    fun testUnresolvedGiven() = codegen(
        """
            fun invoke() = given<String>()
        """
    ) {
        assertCompileError("Unresolved given for String")
    }

    @Test
    fun testNestedUnresolved() = codegen(
        """
            @Given fun bar(foo: @Given Foo = given) = Bar(foo)
            fun invoke() = given<Bar>()
        """
    ) {
        assertCompileError("Unresolved given for Foo")
    }

    @Test
    fun testMultipleGivens() = codegen(
        """
            @Given val a = "a"
            @Given val b = "b"
            fun invoke() = given<String>()
        """
    ) {
        assertCompileError("Multiple givens")
    }

}