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
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiPlatformCodegen
import com.ivianuu.injekt.test.shouldContainMessage
import com.ivianuu.injekt.test.shouldNotContainMessage
import com.ivianuu.injekt.test.source
import io.kotest.matchers.types.shouldBeTypeOf
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
    fun testClassWithMultipleGivenConstructors() = codegen(
        """
            class Dep {
                @Given constructor(@Given foo: Foo)
                @Given constructor(@Given bar: Bar)
            }
        """
    ) {
        compilationShouldHaveFailed("class cannot have multiple @Given constructors")
    }

    @Test
    fun testGivenAnnotationClass() = codegen(
        """
            @Given annotation class MyAnnotation
        """
    ) {
        compilationShouldHaveFailed("annotation class cannot be @Given")
    }

    @Test
    fun testGivenConstructorOnAnnotationClass() = codegen(
        """
            annotation class MyAnnotation @Given constructor()
        """
    ) {
        compilationShouldHaveFailed("annotation class constructor cannot be @Given")
    }

    @Test
    fun testGivenTailrecFunction() = codegen(
        """
            @Given tailrec fun factorial(n : Long, a : Long = 1) : Long {
                return if (n == 1L) a
                else factorial(n - 1, n * a)
            }
        """
    ) {
        compilationShouldHaveFailed("@Given function cannot be tail recursive")
    }

    @Test
    fun testGivenEnumClass() = codegen(
        """
            @Given enum class MyEnum
        """
    ) {
        compilationShouldHaveFailed("enum class cannot be @Given")
    }

    @Test
    fun testGivenSealedClass() = codegen(
        """
            @Given sealed class MyAbstractClass
        """
    ) {
        compilationShouldHaveFailed("abstract @Given cannot be sealed")
    }

    @Test
    fun testGivenInnerClass() = codegen(
        """
            class MyOuterClass {
                @Given
                inner class MyInnerClass
            }
        """
    ) {
        compilationShouldHaveFailed("@Given class cannot be inner")
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

    // todo @Test
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

    @Test
    fun testGivenWithUnresolvableTypeParameter() = codegen(
        """
            @Given
            fun <S> func(): String = ""
        """
    ) {
        compilationShouldHaveFailed("type parameter of a given must be used in the return type or in a upper bound of another type parameter or must be itself marked with @Given")
    }

    @Test
    fun testGivenWithResolvableTypeParameter() = codegen(
        """
            @Given
            fun <@Given T : S, S : Int> lol() {
            }
        """
    )

    @Test
    fun testGivenWithResolvableTypeParameterInQualifier() = codegen(
        """
            @Qualifier annotation class MyQualifier<T>
            @Given
            fun <@Given T : @MyQualifier<U> S, S, U> lol() {
            }
        """
    )

    @Test
    fun testGivenFunctionOverrideWithGivenAnnotation() = codegen(
        """
            abstract class MySuperClass {
                @Given abstract fun foo(): Foo
            }

            @Given
            class MySubClass : MySuperClass() {
                @Given
                override fun foo() = Foo()
            }

            fun invoke() = given<Foo>()
        """
    ) {
        invokeSingleFile()
            .shouldBeTypeOf<Foo>()
    }

    @Test
    fun testFunctionOverrideWithGivenAnnotation() = codegen(
        """
            abstract class MySuperClass {
                abstract fun foo(): Foo
            }

            @Given
            class MySubClass : MySuperClass() {
                @Given
                override fun foo() = Foo()
            }

            fun invoke() = given<Foo>()
        """
    ) {
        invokeSingleFile()
            .shouldBeTypeOf<Foo>()
    }

    @Test
    fun testGivenFunctionOverrideWithoutGivenAnnotation() = codegen(
        """
            abstract class MySuperClass {
                @Given abstract fun foo(): Foo
            }

            class MySubClass : MySuperClass() {
                override fun foo() = Foo()
            }
        """
    ) {
        compilationShouldHaveFailed("'foo' overrides nothing")
    }

    @Test
    fun testNonGivenTypeParameterOverrideWithGivenOverridden() = codegen(
        """
            abstract class MySuperClass {
                @Given abstract fun <@Given T : Bar> foo(): Foo
            }

            class MySubClass : MySuperClass() {
                @Given override fun <T : Bar> foo(): Foo = TODO()
            }
        """
    ) {
        compilationShouldHaveFailed("Conflicting overloads")
    }

    @Test
    fun testGivenPropertyOverrideWithoutGivenAnnotation() = codegen(
        """
            abstract class MySuperClass {
                @Given abstract val foo: Foo
            }

            class MySubClass : MySuperClass() {
                override val foo = Foo()
            }
        """
    ) {
        compilationShouldHaveFailed("'foo' overrides nothing")
    }

    @Test
    fun testActualGivenFunctionWithoutGivenAnnotation() = multiPlatformCodegen(
        commonSources = listOf(
            source(
                """
                    @Given expect fun foo(): Foo 
                """
            )
        ),
        platformSources = listOf(
            source(
                """
                    actual fun foo(): Foo = Foo()
                """
            )
        )
    ) {
        compilationShouldHaveFailed("Actual function 'foo' has no corresponding expected declaration")
    }

    @Test
    fun testActualGivenPropertyWithoutGivenAnnotation() = multiPlatformCodegen(
        commonSources = listOf(
            source(
                """
                    @Given expect val foo: Foo 
                """
            )
        ),
        platformSources = listOf(
            source(
                """
                    actual val foo: Foo = Foo()
                """
            )
        )
    ) {
        compilationShouldHaveFailed("Actual property 'foo' has no corresponding expected declaration")
    }

    @Test
    fun testActualGivenClassWithoutGivenAnnotation() = multiPlatformCodegen(
        commonSources = listOf(
            source(
                """
                    @Given expect class Dep 
                """
            )
        ),
        platformSources = listOf(
            source(
                """
                    actual class Dep
                """
            )
        )
    ) {
        compilationShouldHaveFailed("Actual class 'Dep' has no corresponding expected declaration")
    }

    @Test
    fun testActualGivenConstructorWithoutGivenAnnotation() = multiPlatformCodegen(
        commonSources = listOf(
            source(
                """
                    expect class Dep {
                        @Given constructor()
                    }
                """
            )
        ),
        platformSources = listOf(
            source(
                """
                    actual class Dep {
                        actual constructor()
                    }
                """
            )
        )
    ) {
        compilationShouldHaveFailed("Actual constructor of 'Dep' has no corresponding expected declaration")
    }
}
