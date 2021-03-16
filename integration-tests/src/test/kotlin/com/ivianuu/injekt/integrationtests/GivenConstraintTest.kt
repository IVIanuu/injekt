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
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.shouldNotContainMessage
import com.ivianuu.injekt.test.source
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.Test

class GivenConstraintTest {

    @Test
    fun testGivenWithGivenConstraint() = codegen(
        """
            @Qualifier annotation class Trigger
            @Given fun <@Given T : @Trigger S, S> triggerImpl(@Given instance: T): S = instance

            @Trigger @Given fun foo() = Foo()

            fun invoke() = given<Foo>()
        """
    ) {
        invokeSingleFile()
            .shouldBeTypeOf<Foo>()
    }

    @Test
    fun testSetElementWithGivenConstraint() = codegen(
        """
            @Qualifier annotation class Trigger
            @Given fun <@Given T : @Trigger S, S> triggerImpl(@Given instance: T): S = instance

            @Trigger @Given fun foo() = Foo()

            fun invoke() = given<Set<Foo>>()
        """
    ) {
        1 shouldBe invokeSingleFile<Set<Foo>>().size
    }

    @Test
    fun testModuleWithGivenConstraint() = codegen(
        """
            class MyModule<T : S, S> {
                @Given fun intoSet(@Given instance: T): S = instance
            }
            @Qualifier annotation class Trigger
            @Given fun <@Given T : @Trigger S, S> triggerImpl(@Given instance: T): MyModule<T, S> = MyModule()

            @Trigger @Given fun foo() = Foo()
            @Trigger @Given fun string() = ""

            fun invoke() = given<Set<Foo>>()
        """
    ) {
        1 shouldBe invokeSingleFile<Set<Foo>>().size
    }

    @Test
    fun testGivenConstraintOnNonGivenFunction() = codegen(
        """
            fun <@Given T> triggerImpl() = Unit
        """
    ) {
        compilationShouldHaveFailed("a @Given type constraint is only supported on @Given functions")
    }

    @Test
    fun testGivenConstraintOnNonFunction() = codegen(
        """
            val <@Given T> T.prop get() = Unit
        """
    ) {
        compilationShouldHaveFailed("a @Given type constraint is only supported on @Given functions")
    }

    @Test
    fun testMultipleGivenConstraints() = codegen(
        """
            @Given fun <@Given T, @Given S> triggerImpl() = Unit
        """
    ) {
        compilationShouldHaveFailed("a declaration may have only one @Given type constraint")
    }

    @Test
    fun testGivenConstraintWithQualifierWithTypeParameter() = codegen(
        """
            @Qualifier annotation class Trigger<S>
            @Given fun <@Given @ForTypeKey T : @Trigger<S> Any?, @ForTypeKey S> triggerImpl() = 
                typeKeyOf<S>()

            @Trigger<Bar> @Given fun foo() = Foo()

            fun invoke() = given<TypeKey<Bar>>().value
        """
    ) {
        "com.ivianuu.injekt.test.Bar" shouldBe invokeSingleFile()
    }

    @Test
    fun testGivenConstraintWithQualifierWithTypeParameterMulti() = multiCodegen(
        listOf(
            source(
                """
                    @Qualifier annotation class Trigger<S>
                    @Given fun <@Given @ForTypeKey T : @Trigger<S> Any?, @ForTypeKey S> triggerImpl() = 
                        typeKeyOf<S>()
                """
            )
        ),
        listOf(
            source(
                """
                    @Trigger<Bar> @Given fun foo() = Foo()
                """
            )
        ),
        listOf(
            source(
                """
                    fun <T> givenTypeKeyOf(@Given value: () -> TypeKey<T>) = value()
                    fun invoke() = givenTypeKeyOf<Bar>().value
                """,
                name = "File.kt"
            )
        )
    ) {
        "com.ivianuu.injekt.test.Bar" shouldBe it.invokeSingleFile()
    }

    @Test
    fun testGivenConstraintWithQualifierWithTypeParameterMulti2() = multiCodegen(
        listOf(
            source(
                """
                    @Qualifier annotation class Trigger<S>
                    @Given fun <@Given @ForTypeKey T : @Trigger<S> Any?, @ForTypeKey S> triggerImpl() = 
                        typeKeyOf<S>()
                """
            )
        ),
        listOf(
            source(
                """
                    @Given
                    object FooModule {
                        @Given fun fooModule(): @Trigger<Bar> Foo = Foo()
                    }
                """
            )
        ),
        listOf(
            source(
                """
                    fun <T> givenKeyOf(@Given value: () -> TypeKey<T>) = value()
                    fun invoke() = givenKeyOf<Bar>().value
                """,
                name = "File.kt"
            )
        )
    ) {
        "com.ivianuu.injekt.test.Bar" shouldBe it.invokeSingleFile()
    }

    @Test
    fun testGivenConstraintTriggeredByClass() = codegen(
        """
            @Qualifier annotation class Trigger
            @Given fun <@Given T : @Trigger S, S> triggerImpl(@Given instance: T): S = instance

            @Trigger @Given class NotAny

            fun invoke() = given<NotAny>()
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testGivenConstraintChain() = codegen(
        """
            @Qualifier annotation class A

            @Given fun <@Given T : @A S, S> aImpl() = AModule<S>()

            class AModule<T> {
                @B
                @Given
                fun my(@Given instance: T): T = instance
            }

            @Qualifier annotation class B
            @Given fun <@Given T : @B S, S> bImpl() = BModule<T>()

            class BModule<T> {
                @C
                @Given
                fun my(@Given instance: T): Any? = instance
            }

            @Qualifier annotation class C
            @Given fun <@Given T : @C Any?> cImpl() = Foo()

            @A @Given fun dummy() = 0L
            
            fun invoke() = given<Set<Foo>>().single()
        """
    ) {
        invokeSingleFile()
            .shouldBeTypeOf<Foo>()
    }

    @Test
    fun testScoped() = multiCodegen(
        listOf(
            source(
                """
                    typealias ActivityComponent = Component
                    @Given fun activityComponent(
                        @Given builder: Component.Builder<ActivityComponent>
                    ): ActivityComponent = builder.build()
                    @Given fun appComponent(
                        @Given builder: Component.Builder<AppComponent>
                    ): AppComponent = builder.build()
                """
            )
        ),
        listOf(
            source(
                """
                    @Scoped<AppComponent> @Given fun foo() = Foo()
                    fun invoke() = given<Foo>()
                """,
                name = "File.kt"
            )
        )
    ) {
        it.invokeSingleFile()
            .shouldBeTypeOf<Foo>()
    }

    @Test
    fun testMultipleConstrainedContributionsWithSameType() = codegen(
        """
            @Qualifier annotation class Trigger
            @Given fun <@Given T : @Trigger String> triggerImpl(@Given instance: T): String = instance

            @Trigger @Given fun a() = "a"
            @Trigger @Given fun b() = "b"

            fun invoke() = given<Set<String>>()
        """
    ) {
        invokeSingleFile<Set<String>>()
            .shouldContainExactly("a", "b")
    }

    @Test
    fun testGivenConstraintTypeParameterNotMarkedAsUnused() = codegen(
        """
            @Qualifier annotation class Trigger
            @GivenSetElement fun <@Given T : @Trigger String> triggerImpl(): String = ""
        """
    ) {
        shouldNotContainMessage("Type parameter \"T\" is never used")
    }

    @Test
    fun testNoFinalTypeWarningOnGivenConstraintTypeParameter() = codegen(
        """
            @Qualifier annotation class Trigger
            @GivenSetElement fun <@Given T : @Trigger String> triggerImpl(): String = ""
        """
    ) {
        shouldNotContainMessage("'String' is a final type, and thus a value of the type parameter is predetermined")
    }

    @Test
    fun testCanResolveTypeBasedOnGivenConstraintType() = codegen(
        """
            @Qualifier annotation class Trigger
            @Given fun <@Given T : @Trigger S, S> triggerImpl(
                @Given pair: Pair<S, S>
            ): Int = 0

            @Trigger
            @Given
            val string = ""

            @Given
            fun stringPair() = "a" to "b"

            fun invoke() = given<Int>()
        """
    )

    @Test
    fun testCanResolveTypeWithConstraintTypeArgument() = codegen(
        """
            @Qualifier annotation class Trigger
            @Given fun <@Given T : @Trigger Any?> triggerImpl(
                @Given pair: Pair<T, T>
            ): Int = 0

            @Trigger
            @Given
            val string = ""

            @Given
            fun stringPair() = "a" to "b"

            fun invoke() = given<Int>()
        """
    )

    @Test
    fun testUiDecorator() = codegen(
        """
            typealias UiDecorator = @Composable (@Composable () -> Unit) -> Unit

            @Qualifier annotation class UiDecoratorBinding

            @Given
            fun <@Given T : @UiDecoratorBinding S, @ForTypeKey S : UiDecorator> uiDecoratorBindingImpl(
                @Given instance: T
            ): UiDecorator = instance as UiDecorator

            typealias RootSystemBarsProvider = UiDecorator
            
            @UiDecoratorBinding
            @Given
            fun rootSystemBarsProvider(): RootSystemBarsProvider = {}

            fun invoke() = given<Set<UiDecorator>>().size
        """
    ) {
        1 shouldBe invokeSingleFile()
    }

    @Test
    fun testDivergentConstrainedGiven() = codegen(
        """
            @Given fun <@Given T> constrainedGiven(@Given instance: T): T = instance

            @Given fun foo() = Foo()

            fun invoke() = given<Foo>()
        """
    ) {
        compilationShouldHaveFailed("constrained given return type must not be assignable to the constraint type")
    }

}
