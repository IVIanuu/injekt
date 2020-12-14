package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Bar
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
import org.junit.Test

class GivenResolutionTest {

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
    fun testResolvesInternalGiven() = codegen(
        """
            @Given val foo = Foo()
            fun invoke() = given<Foo>()
        """
    ) {
        assertTrue(invokeSingleFile() is Foo)
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
    fun testResolvesObjectGiven() = codegen(
        """
            object MyObject {
                @Given val foo = Foo()
            }

            fun invoke() = given<Foo>()
        """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testPrefersObjectGivenOverInternalGiven() = codegen(
        """
            @Given lateinit var internalFoo: Foo
            object MyObject {
                @Given lateinit var objectFoo: Foo
                fun resolve() = given<Foo>()
            }

            fun invoke(internal: Foo, objectFoo: Foo): Foo {
                internalFoo = internal
                MyObject.objectFoo = objectFoo
                return MyObject.resolve()
            }
        """
    ) {
        val internal = Foo()
        val objectFoo = Foo()
        val result = invokeSingleFile(internal, objectFoo)
        assertSame(objectFoo, result)
    }

    @Test
    fun testResolvesClassCompanionGiven() = codegen(
        """
            class MyClass {
                fun resolve() = given<Foo>()
                companion object {
                    @Given val foo = Foo()
                }
            }

            fun invoke() = MyClass().resolve()
        """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testPrefersClassCompanionGivenOverInternalGiven() = codegen(
        """
            @Given lateinit var internalFoo: Foo
            class MyClass {
                fun resolve() = given<Foo>()
                companion object {
                    @Given lateinit var companionFoo: Foo
                }
            }

            fun invoke(internal: Foo, companionFoo: Foo): Foo {
                internalFoo = internal
                MyClass.companionFoo = companionFoo
                return MyClass().resolve()
            }
        """
    ) {
        val internal = Foo()
        val companionFoo = Foo()
        val result = invokeSingleFile(internal, companionFoo)
        assertSame(companionFoo, result)
    }

    @Test
    fun testResolvesClassConstructorGiven() = codegen(
        """
            class MyClass(@Given val foo: Foo = Foo()) {
                fun resolve() = given<Foo>()
            }

            fun invoke() = MyClass().resolve()
        """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testResolvesClassGiven() = codegen(
        """
            class MyClass {
                @Given val foo = Foo()
                fun resolve() = given<Foo>()
            }

            fun invoke() = MyClass().resolve()
        """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testPrefersClassGivenOverInternalGiven() = codegen(
        """
            @Given lateinit var internalFoo: Foo
            class MyClass(@Given val classFoo: Foo) {
                fun resolve() = given<Foo>()
            }

            fun invoke(internal: Foo, classFoo: Foo): Foo {
                internalFoo = internal
                return MyClass(classFoo).resolve()
            }
        """
    ) {
        val internal = Foo()
        val classFoo = Foo()
        val result = invokeSingleFile(internal, classFoo)
        assertSame(classFoo, result)
    }

    @Test
    fun testPrefersClassGivenOverClassCompanionGiven() = codegen(
        """
            class MyClass(@Given val classFoo: Foo) {
                fun resolve() = given<Foo>()
                companion object {
                    @Given lateinit var companionFoo: Foo
                }
            }

            fun invoke(classFoo: Foo, companionFoo: Foo): Foo {
                MyClass.companionFoo = companionFoo
                return MyClass(classFoo).resolve()
            }
        """
    ) {
        val classFoo = Foo()
        val companionFoo = Foo()
        val result = invokeSingleFile(classFoo, companionFoo)
        assertSame(classFoo, result)
    }

    // todo class constructor given in init

    @Test
    fun testPrefersFunctionParameterGivenOverInternalGiven() = codegen(
        """
            @Given lateinit var internalFoo: Foo
            fun invoke(internal: Foo, functionFoo: Foo = given): Foo {
                internalFoo = internal
                return given()
            }
        """
    ) {
        val internal = Foo()
        val functionFoo = Foo()
        val result = invokeSingleFile(internal, functionFoo)
        assertSame(functionFoo, result)
    }

    @Test
    fun testPrefersFunctionParameterGivenOverClassGiven() = codegen(
        """
            class MyClass(@Given val classFoo: Foo) {
                fun resolve(@Given functionFoo: Foo) = given<Foo>()
            }

            fun invoke(classFoo: Foo, functionFoo: Foo): Foo {
                return MyClass(classFoo).resolve(functionFoo)
            }
        """
    ) {
        val classFoo = Foo()
        val functionFoo = Foo()
        val result = invokeSingleFile(classFoo, functionFoo)
        assertSame(functionFoo, result)
    }

    @Test
    fun testPrefersFunctionReceiverGivenOverInternalGiven() = codegen(
        """
            @Given lateinit var internalFoo: Foo
            fun @receiver:Given Foo.invoke(internal: Foo): Foo {
                internalFoo = internal
                return given()
            }
        """
    ) {
        val internal = Foo()
        val functionFoo = Foo()
        val result = invokeSingleFile(functionFoo, internal)
        assertSame(functionFoo, result)
    }

    @Test
    fun testPrefersFunctionReceiverGivenOverClassGiven() = codegen(
        """
            class MyClass(@Given val classFoo: Foo) {
                fun @receiver:Given Foo.resolve() = given<Foo>()
            }

            fun invoke(classFoo: Foo, functionFoo: Foo): Foo {
                return with(MyClass(classFoo)) {
                    functionFoo.resolve()
                }
            }
        """
    ) {
        val classFoo = Foo()
        val functionFoo = Foo()
        val result = invokeSingleFile(classFoo, functionFoo)
        assertSame(functionFoo, result)
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
        it.last().assertCompileError("no given argument found of type com.ivianuu.injekt.test.Foo")
    }

    @Test
    fun testUnresolvedGiven() = codegen(
        """
            fun invoke() {
                given<String>()
            }
        """
    ) {
        assertCompileError("no given argument found of type kotlin.String")
    }

    @Test
    fun testNestedUnresolvedGiven() = codegen(
        """
            @Given fun bar(foo: Foo = given) = Bar(foo)
            fun invoke() = given<Bar>()
        """
    ) {
        assertCompileError(" no given argument found of type com.ivianuu.injekt.test.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.bar")
    }

    @Test
    fun testNestedUnresolvedGivenMulti() = multiCodegen(
        listOf(
            source(
                """
                   @Given fun bar(foo: Foo = given) = Bar(foo) 
                """
            )
        ),
        listOf(
            source(
                """
                    fun callee(bar: Bar = given) = bar
                    fun invoke() = callee()
                """
            )
        )
    ) {
        it.last()
            .assertCompileError(" no given argument found of type com.ivianuu.injekt.test.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.bar")
    }

    @Test
    fun testPrefersProviderArgument() = codegen(
        """
            @Given fun foo() = Foo()
            fun invoke(foo: Foo) = given<(Foo) -> Foo>()(foo)
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testPrefersInnerProviderArgumentOverOuterProviderArgument() = codegen(
        """
            @Given fun foo() = Foo()
            fun invoke(foo: Foo) = given<(Foo) -> (Foo) -> Foo>()(Foo())(foo)
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testPrefersResolvableGiven() = codegen(
        """
            @Given fun a() = "a"
            @Given fun b(long: Long = given) = "b"
            fun invoke() = given<String>()
        """
    ) {
        assertEquals("a", invokeSingleFile())
    }

    @Test
    fun testAmbiguousGivens() = codegen(
        """
            @Given val a = "a"
            @Given val b = "b"
            fun invoke() = given<String>()
        """
    ) {
        assertCompileError("ambiguous given arguments of type kotlin.String for parameter value of function com.ivianuu.injekt.given")
    }

    @Test
    fun testPrefersLesserParameters() = codegen(
        """
            @Given val a = "a"
            @Given val foo = Foo()
            @Given fun b(foo: Foo = given) = "b"
            fun invoke() = given<String>()
        """
    ) {
        assertEquals("a", invokeSingleFile())
    }

    @Test
    fun testGenericGiven() = codegen(
        """
            @Given val foo = Foo()
            @Given fun <T> givenList(value: T = given): List<T> = listOf(value)
            fun invoke() = given<List<Foo>>()
        """
    ) {
        val (foo) = invokeSingleFile<List<Any>>()
        assertTrue(foo is Foo)
    }

    @Test
    fun testProviderGiven() = codegen(
        """
            @Given val foo = Foo()
            fun invoke(): Foo {
                return given<() -> Foo>()()
            }
        """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testProviderWithArgsGiven() = codegen(
        """
            @Given fun bar(foo: Foo = given) = Bar(foo)
            fun invoke(): Bar {
                return given<(Foo) -> Bar>()(Foo())
            }
        """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testFunctionInvocationWithGivens() = codegen(
        """
                @Given val foo = Foo()
                fun invoke() {
                    usesFoo()
                }

                fun usesFoo(foo: Foo = given) {
                }
            """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testLocalFunctionInvocationWithGivens() = codegen(
        """
                @Given val foo = Foo()
                fun invoke() {
                    fun usesFoo(foo: Foo = given) {
                    }                    
                    usesFoo()
                }
            """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testConstructorInvocationWithGivens() = codegen(
        """
                @Given val foo = Foo()
                fun invoke() {
                    UsesFoo()
                }

                class UsesFoo(foo: Foo = given)
            """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testLocalConstructorInvocationWithGivens() = codegen(
        """
                @Given val foo = Foo()
                fun invoke() {
                    class UsesFoo(foo: Foo = given)
                    UsesFoo()
                }
            """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testUsesDefaultIfNotGiven() = codegen(
        """
                fun invoke(_foo: Foo): Foo {
                    fun inner(foo: Foo = givenOrElse { _foo }) = foo
                    return inner()
                }
            """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

}
