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

class SpreadingGivenTest {
  @Test fun testSpreadingGivenFunction() = singleAndMultiCodegen(
    """
      @Qualifier annotation class Trigger
      @Provide fun <@Spread T : @Trigger S, S> triggerImpl(instance: T): S = instance

      @Provide fun foo(): @Trigger Foo = Foo()
    """,
    """
        fun invoke() = inject<Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testSpreadingGivenClass() = singleAndMultiCodegen(
    """
      @Provide class MyModule<@Spread T : @Trigger S, S> {
          @Provide fun intoSet(instance: T): @Final S = instance
      }
      @Qualifier annotation class Trigger

      @Qualifier annotation class Final

      @Provide fun foo(): @Trigger Foo = Foo()
      @Provide fun string(): @Trigger String = ""
    """,
    """
      fun invoke() = inject<Set<@Final Foo>>() 
    """
  ) {
    invokeSingleFile<Set<Foo>>().size shouldBe 1
  }

  @Test fun testSpreadingNonGivenClass() = codegen(
    """
      class MyModule<@Spread T>
    """
  ) {
    compilationShouldHaveFailed("a @Spread type parameter is only supported on @Provide functions and @Provide classes")
  }

  @Test fun testSpreadingNonGivenFunction() = codegen(
    """
      fun <@Spread T> triggerImpl() = Unit
    """
  ) {
    compilationShouldHaveFailed("a @Spread type parameter is only supported on @Provide functions and @Provide classes")
  }

  @Test fun testSpreadingProperty() = codegen(
    """
      val <@Spread T> T.prop get() = Unit
    """
  ) {
    compilationShouldHaveFailed("a @Spread type parameter is only supported on @Provide functions and @Provide classes")
  }

  @Test fun testMultipleSpreadTypeParameters() = codegen(
    """
      @Provide fun <@Spread T, @Spread S> triggerImpl() = Unit
    """
  ) {
    compilationShouldHaveFailed("a declaration may have only one @Spread type parameter")
  }

  @Test fun testSpreadingGivenTriggeredByClas() = singleAndMultiCodegen(
    """
      @Qualifier annotation class Trigger
      @Provide fun <@Spread T : @Trigger S, S> triggerImpl(instance: T): S = instance
      
      @Trigger @Provide class NotAny
    """,
    """
      fun invoke() = inject<NotAny>() 
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testSpreadingGivenChain() = singleAndMultiCodegen(
    """
      @Qualifier annotation class A
      
      @Provide fun <@Spread T : @A S, S> aImpl() = AModule<S>()
      
      class AModule<T> {
          @Provide
          fun my(instance: T): @B T = instance
      }
      
      @Qualifier annotation class B
      @Provide fun <@Spread T : @B S, S> bImpl() = BModule<T>()
      
      class BModule<T> {
          @Provide
          fun my(instance: T): @C Any? = instance
      }
      
      @Qualifier annotation class C
      @Provide fun <@Spread T : @C Any?> cImpl() = Foo()
      
      @Provide fun dummy(): @A Long = 0L
    """,
    """
      fun invoke() = inject<Set<Foo>>().single() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testScoped() = singleAndMultiCodegen(
    """
      typealias ActivityGivenScope = GivenScope
      @Provide val activityGivenScopeModule = 
          ChildScopeModule0<AppGivenScope, ActivityGivenScope>()
    """,
    """
      @Provide fun foo(): @Scoped<AppGivenScope> Foo = Foo()
      @ProvideImports("com.ivianuu.injekt.common.*", "com.ivianuu.injekt.scope.*")
      fun invoke() = inject<Foo>()
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testMultipleSpreadCandidatesWithSameType() = singleAndMultiCodegen(
    """
      @Qualifier annotation class Trigger
      @Provide fun <@Spread T : @Trigger String> triggerImpl(instance: T): String = instance

      @Provide fun a(): @Trigger String = "a"
      @Provide fun b(): @Trigger String = "b"
    """,
    """
      fun invoke() = inject<Set<String>>() 
    """
  ) {
    invokeSingleFile<Set<String>>()
      .shouldContainExactly("a", "b")
  }

  @Test fun testSpreadTypeParameterIsNotMarkedAsUnused() = codegen(
    """
      @Qualifier annotation class Trigger
      @ProvideSetElement fun <@Spread T : @Trigger String> triggerImpl(): String = ""
    """
  ) {
    shouldNotContainMessage("Type parameter \"T\" is never used")
  }

  @Test fun testNoFinalTypeWarningOnSpreadTypeParameter() = codegen(
    """
      @Qualifier annotation class Trigger
      @ProvideSetElement fun <@Spread T : @Trigger String> triggerImpl(): String = ""
    """
  ) {
    shouldNotContainMessage("'String' is a final type, and thus a value of the type parameter is predetermined")
  }

  @Test fun testCanResolveTypeBasedOnSpreadConstraintType() = singleAndMultiCodegen(
    """
      @Qualifier annotation class Trigger
      @Provide fun <@Spread T : @Trigger S, S> triggerImpl(pair: Pair<S, S>): Int = 0
      
      @Provide val string: @Trigger String = ""
      
      @Provide fun stringPair() = "a" to "b"
    """,
    """
     fun invoke() = inject<Int>() 
    """
  )

  @Test fun testCanResolveTypeWithSpreadTypeArgument() = singleAndMultiCodegen(
    """
      @Provide fun <@Spread T : String> triggerImpl(pair: Pair<T, T>): Int = 0

      @Provide val string = ""

      @Provide fun stringPair() = "a" to "b"
    """,
    """
      fun invoke() = inject<Int>() 
    """
  )

  @Test fun testUiDecorator() = singleAndMultiCodegen(
    """
      typealias UiDecorator = @Composable (@Composable () -> Unit) -> Unit
  
      @Qualifier annotation class UiDecoratorBinding
  
      @Provide fun <@Spread T : @UiDecoratorBinding S, @ForTypeKey S : UiDecorator> uiDecoratorBindingImpl(
          instance: T
      ): UiDecorator = instance as UiDecorator
  
      typealias RootSystemBarsProvider = UiDecorator
  
      @Provide fun rootSystemBarsProvider(): @UiDecoratorBinding RootSystemBarsProvider = {}
    """,
    """
      fun invoke() = inject<Set<UiDecorator>>().size 
    """
  ) {
    1 shouldBe invokeSingleFile()
  }

  @Test fun testComplexSpreadingSetup() = singleAndMultiCodegen(
    """
      typealias App = Any
  
      @Scoped<AppGivenScope>
      @Provide
      class Dep(app: App)
  
      @Scoped<AppGivenScope>
      @Provide
      class DepWrapper(dep: Dep)
  
      @Scoped<AppGivenScope>
      @Provide
      class DepWrapper2(dep: () -> Dep, wrapper: () -> DepWrapper)
  
      @InstallElement<AppGivenScope>
      @Provide
      class MyComponent(dep: Dep, wrapper: () -> () -> DepWrapper, wrapper2: () -> DepWrapper2)
  
      @Provide
      fun myInitializer(dep: Dep, wrapper: () -> () -> DepWrapper, wrapper2: () -> DepWrapper2): GivenScopeInitializer<AppGivenScope> = {}
    """,
    """
      @ProvideImports("com.ivianuu.injekt.common.*", "com.ivianuu.injekt.scope.*")
      fun invoke() {
          inject<(@Provide @InstallElement<AppGivenScope> App) -> AppGivenScope>()
      }
    """
  )

  @Test fun testSpreadingGivenWithModuleLikeSpreadingReturnType() = singleAndMultiCodegen(
    """
      @Qualifier annotation class ClassSingleton
      
      @Provide inline fun <@Spread T : @ClassSingleton U, reified U : Any> classSingleton(
        factory: () -> T
      ): U = factory()
  
      class MyModule<T : S, S> {
          @Provide fun value(v: T): S = v
      }
  
      @Provide fun <@Spread T : @Qualifier1 S, S> myModule():
          @ClassSingleton MyModule<T, S> = MyModule()
  
      @Provide val foo: @Qualifier1 Foo = Foo()
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    irShouldContain(1, "classSingleton<@ClassSingleton MyModule")
  }

  @Test fun testSpreadingGivenWithModuleLikeSpreadingReturnType2() = singleAndMultiCodegen(
    """
      @Qualifier annotation class ClassSingleton
      
      @Provide inline fun <@Spread T : @ClassSingleton U, reified U : Any> classSingleton(
        factory: () -> T,
        scope: AppGivenScope
      ): U = scope.getOrCreateScopedValue(U::class, factory)
      
      class MyModule<T : S, S> {
        @Provide fun value(v: T): S = v
      }
      
      @Provide fun <@Spread T : @Qualifier1 S, S> myModule(): 
        @ClassSingleton MyModule<T, S> = MyModule()
      
      @Provide val foo: @Qualifier1 Foo = Foo()
    """,
    """
      @ProvideImports("com.ivianuu.injekt.common.*", "com.ivianuu.injekt.scope.*")
      fun invoke() = inject<Set<Foo>>()
    """
  ) {
    irShouldContain(1, "setOf")
  }

  @Test fun testNestedSpreadingGivensWithGenerics() = singleAndMultiCodegen(
    """
      @Qualifier annotation class A<T>
      
      @Provide class Outer<@Spread T : @A<U> S, S, U> {
        @Provide fun <@Spread K : U> inner(): Unit = Unit
      }
      
      @Provide fun dummy(): @A<String> Long = 0L
    """,
    """
      fun invoke() = inject<Unit>() 
    """
  ) {
    compilationShouldHaveFailed("no given argument found of type kotlin.Unit for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testSpreadingGivenWithInvariantTypeParameter() = singleAndMultiCodegen(
    """
      interface IntentKey
      
      typealias KeyIntentFactory<K> = (K) -> Any
      
      @Provide fun <@Spread T : KeyIntentFactory<K>, K : IntentKey> impl() = Foo()
      
      class IntentKeyImpl : IntentKey
      
      @Provide val keyIntentFactoryImpl: KeyIntentFactory<IntentKeyImpl> = { Any() }
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }
}
