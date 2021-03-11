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

import com.ivianuu.injekt.test.Bar
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.compilationShouldBeOk
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldBeTypeOf
import org.jetbrains.kotlin.name.FqName
import org.junit.Test

class GivenResolutionTest {

    @Test
    fun testResolvesExternalGivenInSamePackage() = multiCodegen(
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
        it.invokeSingleFile().shouldBeTypeOf<Foo>()
    }

    @Test
    fun testResolvesExternalGivenInDifferentPackage() = multiCodegen(
        listOf(
            source(
                """
                    @Given val foo = Foo()
                """,
                packageFqName = FqName("givens")
            )
        ),
        listOf(
            source(
                """
                    import givens.*
                    fun invoke() = given<Foo>()
                """,
                name = "File.kt"
            )
        )
    ) {
        it.invokeSingleFile().shouldBeTypeOf<Foo>()
    }

    @Test
    fun testResolvesInternalGivenFromDifferentPackageWithAllUnderImport() = codegen(
        source(
            """
                @Given val foo = Foo()
            """,
            packageFqName = FqName("givens")
        ),
        source(
            """
                import givens.*
                fun invoke() = given<Foo>()
            """,
            name = "File.kt"
        )
    ) {
        invokeSingleFile().shouldBeTypeOf<Foo>()
    }

    @Test
    fun testResolvesInternalGivenFromDifferentPackage() = codegen(
        source(
            """
                @Given val foo = Foo()
            """,
            packageFqName = FqName("givens")
        ),
        source(
            """
                import givens.foo
                fun invoke() = given<Foo>()
            """,
            name = "File.kt"
        )
    ) {
        invokeSingleFile().shouldBeTypeOf<Foo>()
    }

    @Test
    fun testResolvesGivenInSamePackageAndSameFile() = codegen(
        """
            @Given val foo = Foo()
            fun invoke() = given<Foo>()
        """
    ) {
            invokeSingleFile().shouldBeTypeOf<Foo>()
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
        val result = it.invokeSingleFile(internal, external)
        result shouldBeSameInstanceAs internal
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
        objectFoo shouldBeSameInstanceAs result
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
        invokeSingleFile().shouldBeTypeOf<Foo>()
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
        companionFoo shouldBeSameInstanceAs result
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
        invokeSingleFile().shouldBeTypeOf<Foo>()
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
        invokeSingleFile().shouldBeTypeOf<Foo>()
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
        classFoo shouldBeSameInstanceAs result
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
        classFoo shouldBeSameInstanceAs result
    }

    // todo class constructor given in init

    @Test
    fun testPrefersFunctionParameterGivenOverInternalGiven() = codegen(
        """
            @Given lateinit var internalFoo: Foo
            fun invoke(internal: Foo, @Given functionFoo: Foo): Foo {
                internalFoo = internal
                return given()
            }
        """
    ) {
        val internal = Foo()
        val functionFoo = Foo()
        val result = invokeSingleFile(internal, functionFoo)
        functionFoo shouldBeSameInstanceAs result
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
        functionFoo shouldBeSameInstanceAs result
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
        functionFoo shouldBeSameInstanceAs result
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
        functionFoo shouldBeSameInstanceAs result
    }

    @Test
    fun testDerivedGiven() = codegen(
        """
            @Given val foo = Foo()
            @Given val bar = Bar(given())
            fun invoke() = given<Bar>()
        """
    ) {
        invokeSingleFile()
            .shouldBeTypeOf<Bar>()
    }

    @Test
    fun testCanResolveSubTypeOfGiven() = codegen(
        """
            interface Repo
            @Given class RepoImpl : Repo
            fun invoke() = given<Repo>()
        """
    ) {
        invokeSingleFile()
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
        it.last().compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.test.Foo")
    }

    @Test
    fun testUnresolvedGiven() = codegen(
        """
            fun invoke() {
                given<String>()
            }
        """
    ) {
        compilationShouldHaveFailed("no given argument found of type kotlin.String")
    }

    @Test
    fun testNestedUnresolvedGiven() = codegen(
        """
            @Given fun bar(@Given foo: Foo) = Bar(foo)
            fun invoke() = given<Bar>()
        """
    ) {
        compilationShouldHaveFailed(" no given argument found of type com.ivianuu.injekt.test.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.bar")
    }

    @Test
    fun testNestedUnresolvedGivenMulti() = multiCodegen(
        listOf(
            source(
                """
                   @Given fun bar(@Given foo: Foo) = Bar(foo) 
                """
            )
        ),
        listOf(
            source(
                """
                    fun callee(@Given bar: Bar) = bar
                    fun invoke() = callee()
                """
            )
        )
    ) {
        it.last()
            .compilationShouldHaveFailed(" no given argument found of type com.ivianuu.injekt.test.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.bar")
    }

    @Test
    fun testPrefersProviderArgument() = codegen(
        """
            @Given fun foo() = Foo()
            fun invoke(foo: Foo) = given<(@Given Foo) -> Foo>()(foo)
        """
    ) {
        val foo = Foo()
        foo shouldBeSameInstanceAs invokeSingleFile(foo)
    }

    @Test
    fun testPrefersInnerProviderArgumentOverOuterProviderArgument() = codegen(
        """
            @Given fun foo() = Foo()
            fun invoke(foo: Foo) = given<(@Given Foo) -> (@Given Foo) -> Foo>()(Foo())(foo)
        """
    ) {
        val foo = Foo()
        foo shouldBeSameInstanceAs invokeSingleFile(foo)
    }

    @Test
    fun testPrefersResolvableGiven() = codegen(
        """
            @Given fun a() = "a"
            @Given fun b(@Given long: Long) = "b"
            fun invoke() = given<String>()
        """
    ) {
        "a" shouldBe invokeSingleFile()
    }

    @Test
    fun testAmbiguousGivens() = codegen(
        """
            @Given val a = "a"
            @Given val b = "b"
            fun invoke() = given<String>()
        """
    ) {
        compilationShouldHaveFailed("ambiguous given arguments of type kotlin.String for parameter value of function com.ivianuu.injekt.given")
    }

    @Test
    fun testPrefersLesserParameters() = codegen(
        """
            @Given val a = "a"
            @Given val foo = Foo()
            @Given fun b(@Given foo: Foo) = "b"
            fun invoke() = given<String>()
        """
    ) {
        "a" shouldBe invokeSingleFile()
    }

    @Test
    fun testPrefersMoreSpecificType() = codegen(
        """
            @Given fun stringList(): List<String> = listOf("a", "b", "c")
            @Given fun <T> anyList(): List<T> = emptyList()
            fun invoke() = given<List<String>>()
        """
    ) {
        listOf("a", "b", "c") shouldBe invokeSingleFile()
    }

    @Test
    fun testPrefersShorterTree() = codegen(
        """
            @Given val a = "a"
            @Given val foo = Foo()
            @Given fun b(@Given foo: Foo) = "b"
            fun invoke() = given<String>()
        """
    ) {
        "a" shouldBe invokeSingleFile()
    }

    @Test
    fun testPrefersExactCallContext() = codegen(
        """
            @Given lateinit var _foo: Foo
            val suspendFoo = Foo()
            @Given suspend fun suspendFoo() = suspendFoo
            fun invoke(foo: Foo): Foo {
                _foo = foo
                return given()
            }
        """
    ) {
        val foo = Foo()
        foo shouldBeSameInstanceAs invokeSingleFile(foo)
    }

    @Test
    fun testGenericGiven() = codegen(
        """
            @Given val foo = Foo()
            @Given fun <T> givenList(@Given value: T): List<T> = listOf(value)
            fun invoke() = given<List<Foo>>()
        """
    ) {
        val (foo) = invokeSingleFile<List<Any>>()
        foo.shouldBeTypeOf<Foo>()
    }

    @Test
    fun testFunctionInvocationWithGivens() = codegen(
        """
                @Given val foo = Foo()
                fun invoke() {
                    usesFoo()
                }

                fun usesFoo(@Given foo: Foo) {
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
                    fun usesFoo(@Given foo: Foo) {
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

                class UsesFoo(@Given foo: Foo)
            """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testPrimaryConstructorGivenWithReceiver() = codegen(
        """
                fun invoke(foo: Foo) = withGiven(UsesFoo(foo)) {
                    given<Foo>()
                }

                class UsesFoo(@Given val foo: Foo)
            """
    ) {
        val foo = Foo()
        foo shouldBeSameInstanceAs invokeSingleFile(foo)
    }

    @Test
    fun testLocalConstructorInvocationWithGivens() = codegen(
        """
                @Given val foo = Foo()
                fun invoke() {
                    class UsesFoo(@Given foo: Foo)
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
                    fun inner(@Given foo: Foo = _foo) = foo
                    return inner()
                }
            """
    ) {
        val foo = Foo()
        foo shouldBeSameInstanceAs invokeSingleFile(foo)
    }

    @Test
    fun testCanResolveGivenOfGivenThisFunction() = codegen(
        """
            class Dep(@Given val foo: Foo)
            fun invoke(foo: Foo): Foo {
                return withGiven(Dep(foo)) { given<Foo>() }
            }
        """
    ) {
        val foo = Foo()
        foo shouldBeSameInstanceAs invokeSingleFile(foo)
    }

    @Test
    fun testCanResolveGivenWhichDependsOnAssistedGivenOfTheSameType() = codegen(
        """
            typealias SpecialScope = Unit
            
            @Given fun <E> asRunnable(
                @Given factory: (@Given SpecialScope) -> List<E>
            ): List<E> = factory(Unit)
            
            @Given fun raw(@Given scope: SpecialScope): List<String> = listOf("")
            
            fun main() {
                given<List<String>>()
            } 
        """
    )

    @Test
    fun testCanResolveStarProjectedType() = codegen(
        """
            @Given fun foos() = Foo() to Foo()
            
            @Qualifier annotation class First
            @Given fun <A : @First B, B> first(@Given pair: Pair<B, *>): A = pair.first as A

            fun invoke() = given<@First Foo>()
        """
    )

    @Test
    fun testCannotResolveObjectWithoutGiven() = codegen(
        """
            object MyObject
            fun invoke() = given<MyObject>()
        """
    ) {
        compilationShouldHaveFailed("no given argument")
    }

    @Test
    fun testCanResolveObjectWithGiven() = codegen(
        """
            @Given object MyObject
            fun invoke() = given<MyObject>()
        """
    )

    @Test
    fun testCannotResolveExternalInternalGiven() = multiCodegen(
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
                """
            )
        )
    ) {
        it.last().compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.test.Foo")
    }

    @Test
    fun testCannotResolvePrivateGivenFromOuterScope() = codegen(
        """
                @Given class FooHolder {
                    @Given private val foo = Foo()
                }
                fun invoke() = given<Foo>()
                """
    ) {
        compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.test.Foo")
    }

    @Test
    fun testCanResolvePrivateGivenFromInnerScope() = codegen(
        """
                @Given class FooHolder {
                    @Given private val foo = Foo()
                    fun invoke() = given<Foo>()
                }
                """
    ) {
        compilationShouldBeOk()
    }

    @Test
    fun testCannotResolveProtectedGivenFromOuterScope() = codegen(
        """
                @Given open class FooHolder {
                    @Given protected val foo = Foo()
                }
                fun invoke() = given<Foo>()
                """
    ) {
        compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.test.Foo")
    }

    @Test
    fun testCanResolveProtectedGivenFromSameClass() = codegen(
        """
                @Given open class FooHolder {
                    @Given protected val foo = Foo()
                    fun invoke() = given<Foo>()
                }
                """
    ) {
        compilationShouldBeOk()
    }

    @Test
    fun testCanResolveProtectedGivenFromSubClass() = codegen(
        """
                abstract class AbstractFooHolder {
                    @Given protected val foo = Foo()
                }
                class FooHolderImpl : AbstractFooHolder() {
                    fun invoke() = given<Foo>()
                }
                """
    ) {
        compilationShouldBeOk()
    }

}
