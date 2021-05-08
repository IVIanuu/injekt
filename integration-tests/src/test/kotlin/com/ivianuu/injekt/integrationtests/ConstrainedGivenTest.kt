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
import io.kotest.matchers.*
import io.kotest.matchers.collections.*
import io.kotest.matchers.types.*
import org.junit.*

class ConstrainedGivenTest {
    @Test
    fun testGivenWithGivenConstraint() = singleAndMultiCodegen(
        """
            @Qualifier annotation class Trigger
            @Given fun <@Given T : @Trigger S, S> triggerImpl(@Given instance: T): S = instance

            @Given fun foo(): @Trigger Foo = Foo()
        """,
        """
           fun invoke() = given<Foo>() 
        """
    ) {
        invokeSingleFile().shouldBeTypeOf<Foo>()
    }

    @Test
    fun testClassWithGivenConstraint() = singleAndMultiCodegen(
        """
            @Given
            class MyModule<@Given T : @Trigger S, S> {
                @Given fun intoSet(@Given instance: T): @Final S = instance
            }
            @Qualifier annotation class Trigger

            @Qualifier annotation class Final

            @Given fun foo(): @Trigger Foo = Foo()
            @Given fun string(): @Trigger String = ""
        """,
        """
           fun invoke() = given<Set<@Final Foo>>() 
        """
    ) {
        invokeSingleFile<Set<Foo>>().size shouldBe 1
    }

    @Test
    fun testGivenConstraintOnNonGivenClass() = codegen(
        """
            class MyModule<@Given T>
        """
    ) {
        compilationShouldHaveFailed("a @Given type constraint is only supported on @Given functions and @Given classes")
    }

    @Test
    fun testGivenConstraintOnNonGivenFunction() = codegen(
        """
            fun <@Given T> triggerImpl() = Unit
        """
    ) {
        compilationShouldHaveFailed("a @Given type constraint is only supported on @Given functions and @Given classes")
    }

    @Test
    fun testGivenConstraintOnNonFunction() = codegen(
        """
            val <@Given T> T.prop get() = Unit
        """
    ) {
        compilationShouldHaveFailed("a @Given type constraint is only supported on @Given functions and @Given classes")
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
    fun testGivenConstraintTriggeredByClass() = singleAndMultiCodegen(
        """
            @Qualifier annotation class Trigger
            @Given fun <@Given T : @Trigger S, S> triggerImpl(@Given instance: T): S = instance

            @Trigger @Given class NotAny
        """,
        """
           fun invoke() = given<NotAny>() 
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testGivenConstraintChain() = singleAndMultiCodegen(
        """
            @Qualifier annotation class A

            @Given fun <@Given T : @A S, S> aImpl() = AModule<S>()

            class AModule<T> {
                @Given
                fun my(@Given instance: T): @B T = instance
            }

            @Qualifier annotation class B
            @Given fun <@Given T : @B S, S> bImpl() = BModule<T>()

            class BModule<T> {
                @Given
                fun my(@Given instance: T): @C Any? = instance
            }

            @Qualifier annotation class C
            @Given fun <@Given T : @C Any?> cImpl() = Foo()

            @Given fun dummy(): @A Long = 0L
        """,
        """
           fun invoke() = given<Set<Foo>>().single() 
        """
    ) {
        invokeSingleFile().shouldBeTypeOf<Foo>()
    }

    @Test
    fun testScoped() = singleAndMultiCodegen(
    """
            typealias ActivityGivenScope = GivenScope
            @Given val activityGivenScopeModule = 
                ChildGivenScopeModule0<AppGivenScope, ActivityGivenScope>()
            """,
    """
            @Given fun foo(): @Scoped<AppGivenScope> Foo = Foo()
            @GivenImports("com.ivianuu.injekt.common.*", "com.ivianuu.injekt.scope.*")
            fun invoke() = given<Foo>()
            """
    ) {
        invokeSingleFile().shouldBeTypeOf<Foo>()
    }

    @Test
    fun testMultipleConstrainedContributionsWithSameType() = singleAndMultiCodegen(
        """
            @Qualifier annotation class Trigger
            @Given fun <@Given T : @Trigger String> triggerImpl(@Given instance: T): String = instance

            @Given fun a(): @Trigger String = "a"
            @Given fun b(): @Trigger String = "b"
        """,
        """
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
    fun testCanResolveTypeBasedOnGivenConstraintType() = singleAndMultiCodegen(
        """
            @Qualifier annotation class Trigger
            @Given fun <@Given T : @Trigger S, S> triggerImpl(
                @Given pair: Pair<S, S>
            ): Int = 0

            @Given
            val string: @Trigger String = ""

            @Given
            fun stringPair() = "a" to "b"
        """,
        """
           fun invoke() = given<Int>() 
        """
    )

    @Test
    fun testCanResolveTypeWithConstraintTypeArgument() = singleAndMultiCodegen(
        """
            @Given fun <@Given T : String> triggerImpl(
                @Given pair: Pair<T, T>
            ): Int = 0

            @Given
            val string = ""

            @Given
            fun stringPair() = "a" to "b"
        """,
        """
           fun invoke() = given<Int>() 
        """
    )

    @Test
    fun testUiDecorator() = singleAndMultiCodegen(
        """
            typealias UiDecorator = @Composable (@Composable () -> Unit) -> Unit

            @Qualifier annotation class UiDecoratorBinding

            @Given
            fun <@Given T : @UiDecoratorBinding S, @ForTypeKey S : UiDecorator> uiDecoratorBindingImpl(
                @Given instance: T
            ): UiDecorator = instance as UiDecorator

            typealias RootSystemBarsProvider = UiDecorator

            @Given
            fun rootSystemBarsProvider(): @UiDecoratorBinding RootSystemBarsProvider = {}
        """,
        """
           fun invoke() = given<Set<UiDecorator>>().size 
        """
    ) {
        1 shouldBe invokeSingleFile()
    }

    @Test
    fun testComplexGivenConstraintSetup() = singleAndMultiCodegen(
        """
            typealias App = Any

            @Scoped<AppGivenScope>
            @Given
            class Dep(@Given app: App)

            @Scoped<AppGivenScope>
            @Given
            class DepWrapper(@Given dep: Dep)

            @Scoped<AppGivenScope>
            @Given
            class DepWrapper2(@Given dep: () -> Dep, @Given wrapper: () -> DepWrapper)

            @InstallElement<AppGivenScope>
            @Given
            class MyComponent(@Given dep: Dep, @Given wrapper: () -> () -> DepWrapper, @Given wrapper2: () -> DepWrapper2)

            @Given
            fun myInitializer(@Given dep: Dep, @Given wrapper: () -> () -> DepWrapper, @Given wrapper2: () -> DepWrapper2): GivenScopeInitializer<AppGivenScope> = {}
        """,
        """
            @GivenImports("com.ivianuu.injekt.common.*", "com.ivianuu.injekt.scope.*")
            fun invoke() {
                given<(@Given @InstallElement<AppGivenScope> App) -> AppGivenScope>()
            } 
        """
    )

    @Test
    fun testConstrainedGivenWithModuleLikeConstrainedReturnType() = singleAndMultiCodegen(
        """
            @Qualifier
            annotation class ClassSingleton
            
            @Given
            inline fun <@Given T : @ClassSingleton U, reified U : Any> classSingleton(
                @Given factory: () -> T
            ): U = factory()

            class MyModule<T : S, S> {
                @Given fun value(@Given v: T): S = v
            }

            @Given fun <@Given T : @Qualifier1 S, S> myModule():
                @ClassSingleton MyModule<T, S> = MyModule()

            @Given val foo: @Qualifier1 Foo = Foo()
        """,
        """
            fun invoke() = given<Foo>() 
        """
    ) {
        irShouldContain(1, "classSingleton<@ClassSingleton MyModule")
    }

    @Test
    fun testConstrainedGivenWithModuleLikeConstrainedReturnType2() = singleAndMultiCodegen(
        """
            @Qualifier
            annotation class ClassSingleton
            
            @Given
            inline fun <@Given T : @ClassSingleton U, reified U : Any> classSingleton(
                @Given factory: () -> T,
                @Given scope: AppGivenScope
            ): U = scope.getOrCreateScopedValue(U::class, factory)

            class MyModule<T : S, S> {
                @Given fun value(@Given v: T): S = v
            }

            @Given fun <@Given T : @Qualifier1 S, S> myModule():
                @ClassSingleton MyModule<T, S> = MyModule()

            @Given val foo: @Qualifier1 Foo = Foo()
        """,
        """
            @GivenImports("com.ivianuu.injekt.common.*", "com.ivianuu.injekt.scope.*")
            fun invoke() = given<Set<Foo>>() 
        """
    ) {
        irShouldContain(1, "setOf")
    }

    @Test
    fun testNestedConstrainedGivensWithGenerics() = singleAndMultiCodegen(
        """
            @Qualifier annotation class A<T>

            @Given class Outer<@Given T : @A<U> S, S, U> {
                @Given fun <@Given K : U> inner(): Unit = Unit
            }

            @Given fun dummy(): @A<String> Long = 0L
        """,
        """
           fun invoke() = given<Unit>() 
        """
    ) {
        compilationShouldHaveFailed("no given argument found of type kotlin.Unit for parameter value of function com.ivianuu.injekt.given")
    }

    @Test
    fun testConstrainedGivenWithInvariantTypeParameter() = singleAndMultiCodegen(
        """
            interface IntentKey

            typealias KeyIntentFactory<K> = (K) -> Any
    
            @Given 
            fun <@Given T : KeyIntentFactory<K>, K : IntentKey> impl() = Foo()

            class IntentKeyImpl : IntentKey

            @Given
            val keyIntentFactoryImpl: KeyIntentFactory<IntentKeyImpl> = { Any() }
        """,
        """
           fun invoke() = given<Foo>() 
        """
    ) {
        invokeSingleFile().shouldBeTypeOf<Foo>()
    }
}
