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

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.compilationShouldBeOk
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.irShouldContain
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.singleAndMultiCodegen
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.Test

class GivenResolutionTest {
    @Test
    fun testPrefersInternalGivenOverExternal() = multiCodegen(
        """
            @Given lateinit var externalFoo: Foo
            """,
        """
            @Given lateinit var internalFoo: Foo

            fun invoke(internal: Foo, external: Foo): Foo {
                externalFoo = external
                internalFoo = internal
                return given<Foo>()
            }
            """
    ) {
        val internal = Foo()
        val external = Foo()
        val result = invokeSingleFile(internal, external)
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

    @Test
    fun testPrefersConstructorParameterGivenOverClassBodyGiven() = codegen(
        """
            lateinit var classBodyFoo: Foo
            class MyClass(@Given constructorFoo: Foo) {
                val finalFoo = given<Foo>()
                @Given val classFoo get() = classBodyFoo
            }

            fun invoke(constructorFoo: Foo, _classBodyFoo: Foo): Foo {
                classBodyFoo = _classBodyFoo
                return MyClass(constructorFoo).finalFoo
            }
        """
    ) {
        val constructorFoo = Foo()
        val classBodyFoo = Foo()
        val result = invokeSingleFile(constructorFoo, classBodyFoo)
        result shouldBeSameInstanceAs constructorFoo
    }

    @Test
    fun testPrefersSubClassGivenOverSuperClassGiven() = codegen(
        """
            abstract class MySuperClass(@Given val superClassFoo: Foo)
            class MySubClass(@Given val subClassFoo: Foo, superClassFoo: Foo) : MySuperClass(superClassFoo) {
                fun finalFoo(): Foo = given()
            }

            fun invoke(subClassFoo: Foo, superClassFoo: Foo): Foo {
                return MySubClass(subClassFoo, superClassFoo).finalFoo()
            }
        """
    ) {
        val subClassFoo = Foo()
        val superClassFoo = Foo()
        val result = invokeSingleFile(subClassFoo, superClassFoo)
        result shouldBeSameInstanceAs subClassFoo
    }

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
            fun Foo.invoke(internal: Foo): Foo {
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
                fun Foo.resolve() = given<Foo>()
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
    fun testPrefersHigherPriority() = codegen(
        """
            @Given @Priority(1) val high = "high"
            @Given @Priority(0) val low = "low"
            fun invoke(): String = given()
        """
    ) {
        invokeSingleFile() shouldBe "high"
    }

    @Test
    fun testAmbiguousGivens() = codegen(
        """
            @Given val a = "a"
            @Given val b = "b"
            fun invoke() = given<String>()
        """
    ) {
        compilationShouldHaveFailed("ambiguous given arguments:\n" +
                "com.ivianuu.injekt.integrationtests.a\n" +
                "com.ivianuu.injekt.integrationtests.b\n" +
                "do all match type kotlin.String for parameter value of function com.ivianuu.injekt.given")
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
    fun testPrefersMoreSpecificType2() = codegen(
        """
            @Given fun <T> list(): List<T> = emptyList()
            @Given fun <T> listList(): List<List<T>> = listOf(listOf("a", "b", "c")) as List<List<T>>
            fun invoke() = given<List<List<String>>>()
        """
    ) {
        invokeSingleFile() shouldBe listOf(listOf("a", "b", "c"))
    }

    @Test
    fun testPrefersNonNullType() = codegen(
        """
            @Given val nonNull = "nonnull"
            @Given val nullable: String? = "nullable"
            fun invoke() = given<String?>()
        """
    ) {
        invokeSingleFile() shouldBe "nonnull"
    }

    @Test
    fun testPrefersGivenFromAGivenConstraint() = codegen(
        """
            @MyQualifier
            @Given 
            class FooModule {
                @Given
                val foo = Foo()
            }

            @Qualifier
            annotation class MyQualifier

            @Given
            fun <@Given T : @MyQualifier S, S> myQualifier(@Given instance: T): S = instance

            fun invoke() = given<Foo>()
        """
    )

    @Test
    fun testPrefersModuleGivenConstraint() = singleAndMultiCodegen(
        """
            @Given class MyModule<@Given T : @Qualifier1 S, S> {
                @Given fun unqualified(@Given v: T): S = v 
            }
                """,
        """
            fun invoke() = given<(@Given @Qualifier1 TypeKey<Foo>) -> TypeKey<Foo>>() 
                """
    )

    @Test
    fun testPrefersGivenConstraintWithBetterTypeOverNonGivenConstraint() = codegen(
        """
            typealias Collector<T> = (T) -> Unit
        
            @Given
            fun <T> worseCollector(): Collector<T> = {}

            @Qualifier
            annotation class MyScoped<S : GivenScope>

            @Given
            inline fun <@Given T : @MyScoped<S> U, @ForTypeKey U : Any, S : GivenScope> myScopedImpl(
                @Given scope: S,
                @Given factory: () -> T
            ): U = scope.getOrCreateScopedValue<U>(factory)

            @Given
            fun <T> betterCollector(): @MyScoped<AppGivenScope> Collector<List<T>> = {}

            fun invoke() {
                @Given val appGivenScope = given<AppGivenScope>()
                given<Collector<List<String>>>()
            }
        """
    ) {
        irShouldContain(1, "return betterCollector<String>()")
    }

    @Test
    fun testPrefersUserGivenWithWorseTypeOverFrameworkGiven() = codegen(
        """
            class MyElementModule<T : S, S> {
                @Given
                fun userProvider(@Given value: () -> T): () -> S = value
            }

            @Qualifier
            annotation class MyElement

            @Given
            fun <@Given T : @MyElement S, S> myElementModule() = MyElementModule<T, S>()

            @Given
            val myElementFoo: @MyElement Foo = Foo()
        
            @Given
            val foo = Foo()

            fun invoke() = given<() -> Foo>()
        """
    )

    @Test
    fun testDoesNotUseFrameworkGivensIfThereAreUserGivens() = codegen(
        """
            fun <T> diyProvider(@Given unit: Unit): () -> T = { TODO() } 

            fun invoke() = given<() -> Foo>()
        """
    ) {
        compilationShouldHaveFailed("no given argument found of type kotlin.Function0<com.ivianuu.injekt.test.Foo>")
    }

    @Test
    fun testUsesDefaultValueIfNoCandidateExists() = codegen(
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
    fun testDoesNotUseDefaultValueIfCandidateHasFailures() = codegen(
        """
                @Given fun bar(@Given foo: Foo) = Bar(foo)
                fun invoke() {
                    fun inner(@Given bar: Bar = Bar(Foo())) = bar
                    return inner()
                }
            """
    ) {
        compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.test.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.bar")
    }

    @Test
    fun testDoesUseDefaultValueIfCandidateHasFailuresButHasUseDefaultValueOnAllError() = codegen(
        """
                @Given fun bar(@Given foo: Foo) = Bar(foo)
                fun invoke(foo: Foo): Foo {
                    fun inner(@Given @DefaultOnAllErrors bar: Bar = Bar(foo)) = bar
                    return inner().foo
                }
            """
    ) {
        val foo = Foo()
        foo shouldBeSameInstanceAs invokeSingleFile(foo)
    }

    @Test
    fun testPrefersSubTypeParameters() = codegen(
        """
            import com.ivianuu.injekt.integrationtests.Priority.*
            sealed class Priority {
                @Given open class Low : Priority()
                @Given class High : Low()
            }

            @Given fun value1(@Given value: Priority.Low) = "low"
            @Given fun value2(@Given value: Priority.High) = "high"

            fun invoke() = given<String>()
        """
    )
}
