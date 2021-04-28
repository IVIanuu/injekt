/*
 * Copyright 2021 Manuel Wrage
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

import com.ivianuu.injekt.test.*
import io.kotest.matchers.types.*
import org.jetbrains.kotlin.name.*
import org.junit.*

class GivenResolveTest {
    @Test
    fun testResolvesExternalGivenInSamePackage() = multiCodegen(
        """
            @Given val foo = Foo()
        """,
        """
            fun invoke() = given<Foo>()
                """
    ) {
        invokeSingleFile().shouldBeTypeOf<Foo>()
    }

    @Test
    fun testResolvesExternalGivenInDifferentPackage() = multiCodegen(
        listOf(
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
                    fun invoke() = given<Foo>()
                    """,
                    name = "File.kt",
                    givenImports = listOf("givens.*")
                )
            )
        )
    ) {
        invokeSingleFile().shouldBeTypeOf<Foo>()
    }

    @Test
    fun testResolvesInternalGivenFromDifferentPackageWithAllUnderImport() = codegen(
        listOf(
            source(
                """
                @Given val foo = Foo()
            """,
                packageFqName = FqName("givens")
            ),
            source(
                """
                    fun invoke() = given<Foo>()
                    """,
                name = "File.kt",
                givenImports = listOf("givens.*")
            )
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
    fun testResolvesClassCompanionGivenFromWithinTheClass() = codegen(
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
    fun testResolvesClassCompanionGivenFromOuterClass() = codegen(
        """
            class MyClass {
                companion object {
                    @Given val foo = Foo()
                }
            }

            fun invoke() = given<Foo>()
        """
    ) {
        invokeSingleFile().shouldBeTypeOf<Foo>()
    }

    @Test
    fun testResolvesClassCompanionClassGivenFromOuterClass() = codegen(
        """
            class MyClass {
                companion object {
                    @Given class MyModule {
                        @Given val foo = Foo()
                    }
                }
            }

            fun invoke() = given<Foo>()
        """
    ) {
        invokeSingleFile().shouldBeTypeOf<Foo>()
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
    fun testDerivedGiven() = codegen(
        """
            @Given val foo = Foo()
            @Given val bar: Bar = Bar(given())
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
    fun testNestedUnresolvedGiven() = singleAndMultiCodegen(
        """
            @Given fun bar(@Given foo: Foo) = Bar(foo)
        """,
        """
           fun invoke() = given<Bar>() 
        """
    ) {
        compilationShouldHaveFailed(" no given argument found of type com.ivianuu.injekt.test.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.bar")
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
    fun testCanResolveInternalGivenIfRequestedFromTheSameModule() = multiCodegen(
        """
            
            @Given
            internal val foo = Foo()
            @Given fun bar(@Given foo: Foo) = Bar(foo)
        """,
        """
            fun invoke() = given<Bar>()
        """
    )

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
                fun invoke(foo: Foo) = with(UsesFoo(foo)) {
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
    fun testCanResolveGivenOfGivenThisFunction() = codegen(
        """
            class Dep(@Given val foo: Foo)
            fun invoke(foo: Foo): Foo {
                return with(Dep(foo)) { given<Foo>() }
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
        """
            @Given internal val foo = Foo()
            """,
        """
            fun invoke() = given<Foo>()
            """
    ) {
        compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.test.Foo")
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
    )

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
    )

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
    )

    @Test
    fun testCannotResolvePropertyWithTheSameNameAsAnGivenPrimaryConstructorParameter() = codegen(
        """
            @Given class MyClass(@Given foo: Foo) {
                val foo = foo
            }

            fun invoke() = given<Foo>()
        """
    ) {
        compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.given")
    }

    @Test
    fun testCannotResolveGivenConstructorParameterOfGivenClassFromOutsideTheClass() = codegen(
        """
            @Given class MyClass(@Given val foo: Foo)
            fun invoke() = given<Foo>()
        """
    ) {
        compilationShouldHaveFailed(
            "no given argument found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.given"
        )
    }

    @Test
    fun testCanResolveGivenConstructorParameterOfNonGivenClassFromOutsideTheClass() = codegen(
        """
            class MyClass(@Given val foo: Foo)
            fun invoke(@Given myClass: MyClass) = given<Foo>()
        """
    )

    @Test
    fun testCanResolveGivenConstructorParameterFromInsideTheClass() = codegen(
        """
            @Given class MyClass(@Given val foo: Foo) {
                fun invoke() = given<Foo>()
            }
        """
    )

    @Test
    fun testResolvesGivenWithTypeParameterInScope() = codegen(
        """
            @Given fun <T> list(): List<T> = emptyList()
            fun <T> invoke() {
                given<List<T>>()
            }
        """
    )

    @Test
    fun testCannotUseNonReifiedTypeParameterForReifiedGiven() = codegen(
        """
            @Given inline fun <reified T> list(): List<T> {
                T::class
                return emptyList()
            }
            fun <T> invoke() {
                given<List<T>>()
            }
        """
    ) {
        compilationShouldHaveFailed(
            "type parameter T of given argument com.ivianuu.injekt.integrationtests.list() of type kotlin.collections.List<T> for parameter value of function com.ivianuu.injekt.given is marked with reified but type argument com.ivianuu.injekt.integrationtests.invoke.T is not marked with reified"
        )
    }

    @Test
    fun testCannotUseNonForTypeKeyTypeParameterForForTypeKeyGiven() = codegen(
        """
            @Given fun <@ForTypeKey T> list(): List<T> {
                typeKeyOf<T>()
                return emptyList()
            }
            fun <T> invoke() {
                given<List<T>>()
            }
        """
    ) {
        compilationShouldHaveFailed(
            "type parameter T of given argument com.ivianuu.injekt.integrationtests.list() of type kotlin.collections.List<T> for parameter value of function com.ivianuu.injekt.given is marked with @ForTypeKey but type argument com.ivianuu.injekt.integrationtests.invoke.T is not marked with @ForTypeKey"
        )
    }

    @Test
    fun testCannotResolveUnparameterizedSubTypeOfParameterizedGiven() = codegen(
        """
            typealias TypedString<T> = String

            @Given
            val foo = Foo()

            @Given
            fun <T : Foo> typedString(@Given value: T): TypedString<T> = ""

            fun invoke() = given<String>()
        """
    )

    @Test
    fun testCannotResolveUnparameterizedSubTypeOfParameterizedGivenWithQualifiers() = codegen(
        """
            typealias TypedString<T> = String

            @Given
            val foo = Foo()

            @Given
            fun <T : Foo> typedString(@Given value: T): @TypedQualifier<T> TypedString<T> = ""

            fun invoke() = given<@TypedQualifier<Foo> String>()
        """
    )
}