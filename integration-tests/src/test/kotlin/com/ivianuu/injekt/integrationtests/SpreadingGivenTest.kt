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
      @Given fun <@Spread T : @Trigger S, S> triggerImpl(instance: T): S = instance

      @Given fun foo(): @Trigger Foo = Foo()
    """,
    """
        fun invoke() = summon<Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testSpreadingGivenClass() = singleAndMultiCodegen(
    """
      @Given class MyModule<@Spread T : @Trigger S, S> {
          @Given fun intoSet(instance: T): @Final S = instance
      }
      @Qualifier annotation class Trigger

      @Qualifier annotation class Final

      @Given fun foo(): @Trigger Foo = Foo()
      @Given fun string(): @Trigger String = ""
    """,
    """
      fun invoke() = summon<Set<@Final Foo>>() 
    """
  ) {
    invokeSingleFile<Set<Foo>>().size shouldBe 1
  }

  @Test fun testSpreadingNonGivenClass() = codegen(
    """
      class MyModule<@Spread T>
    """
  ) {
    compilationShouldHaveFailed("a @Spread type parameter is only supported on @Given functions and @Given classes")
  }

  @Test fun testSpreadingNonGivenFunction() = codegen(
    """
      fun <@Spread T> triggerImpl() = Unit
    """
  ) {
    compilationShouldHaveFailed("a @Spread type parameter is only supported on @Given functions and @Given classes")
  }

  @Test fun testSpreadingProperty() = codegen(
    """
      val <@Spread T> T.prop get() = Unit
    """
  ) {
    compilationShouldHaveFailed("a @Spread type parameter is only supported on @Given functions and @Given classes")
  }

  @Test fun testMultipleSpreadTypeParameters() = codegen(
    """
      @Given fun <@Spread T, @Spread S> triggerImpl() = Unit
    """
  ) {
    compilationShouldHaveFailed("a declaration may have only one @Spread type parameter")
  }

  @Test fun testSpreadingGivenTriggeredByClas() = singleAndMultiCodegen(
    """
      @Qualifier annotation class Trigger
      @Given fun <@Spread T : @Trigger S, S> triggerImpl(instance: T): S = instance
      
      @Trigger @Given class NotAny
    """,
    """
      fun invoke() = summon<NotAny>() 
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testSpreadingGivenChain() = singleAndMultiCodegen(
    """
      @Qualifier annotation class A
      
      @Given fun <@Spread T : @A S, S> aImpl() = AModule<S>()
      
      class AModule<T> {
          @Given
          fun my(instance: T): @B T = instance
      }
      
      @Qualifier annotation class B
      @Given fun <@Spread T : @B S, S> bImpl() = BModule<T>()
      
      class BModule<T> {
          @Given
          fun my(instance: T): @C Any? = instance
      }
      
      @Qualifier annotation class C
      @Given fun <@Spread T : @C Any?> cImpl() = Foo()
      
      @Given fun dummy(): @A Long = 0L
    """,
    """
      fun invoke() = summon<Set<Foo>>().single() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testScoped() = singleAndMultiCodegen(
    """
      typealias ActivityGivenScope = GivenScope
      @Given val activityGivenScopeModule = 
          ChildScopeModule0<AppGivenScope, ActivityGivenScope>()
    """,
    """
      @Given fun foo(): @Scoped<AppGivenScope> Foo = Foo()
      @GivenImports("com.ivianuu.injekt.common.*", "com.ivianuu.injekt.scope.*")
      fun invoke() = summon<Foo>()
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testMultipleSpreadCandidatesWithSameType() = singleAndMultiCodegen(
    """
      @Qualifier annotation class Trigger
      @Given fun <@Spread T : @Trigger String> triggerImpl(instance: T): String = instance

      @Given fun a(): @Trigger String = "a"
      @Given fun b(): @Trigger String = "b"
    """,
    """
      fun invoke() = summon<Set<String>>() 
    """
  ) {
    invokeSingleFile<Set<String>>()
      .shouldContainExactly("a", "b")
  }

  @Test fun testSpreadTypeParameterIsNotMarkedAsUnused() = codegen(
    """
      @Qualifier annotation class Trigger
      @GivenSetElement fun <@Spread T : @Trigger String> triggerImpl(): String = ""
    """
  ) {
    shouldNotContainMessage("Type parameter \"T\" is never used")
  }

  @Test fun testNoFinalTypeWarningOnSpreadTypeParameter() = codegen(
    """
      @Qualifier annotation class Trigger
      @GivenSetElement fun <@Spread T : @Trigger String> triggerImpl(): String = ""
    """
  ) {
    shouldNotContainMessage("'String' is a final type, and thus a value of the type parameter is predetermined")
  }

  @Test fun testCanResolveTypeBasedOnSpreadConstraintType() = singleAndMultiCodegen(
    """
      @Qualifier annotation class Trigger
      @Given fun <@Spread T : @Trigger S, S> triggerImpl(pair: Pair<S, S>): Int = 0
      
      @Given val string: @Trigger String = ""
      
      @Given fun stringPair() = "a" to "b"
    """,
    """
     fun invoke() = summon<Int>() 
    """
  )

  @Test fun testCanResolveTypeWithSpreadTypeArgument() = singleAndMultiCodegen(
    """
      @Given fun <@Spread T : String> triggerImpl(pair: Pair<T, T>): Int = 0

      @Given val string = ""

      @Given fun stringPair() = "a" to "b"
    """,
    """
      fun invoke() = summon<Int>() 
    """
  )

  @Test fun testUiDecorator() = singleAndMultiCodegen(
    """
      typealias UiDecorator = @Composable (@Composable () -> Unit) -> Unit
  
      @Qualifier annotation class UiDecoratorBinding
  
      @Given fun <@Spread T : @UiDecoratorBinding S, @ForTypeKey S : UiDecorator> uiDecoratorBindingImpl(
          instance: T
      ): UiDecorator = instance as UiDecorator
  
      typealias RootSystemBarsProvider = UiDecorator
  
      @Given fun rootSystemBarsProvider(): @UiDecoratorBinding RootSystemBarsProvider = {}
    """,
    """
      fun invoke() = summon<Set<UiDecorator>>().size 
    """
  ) {
    1 shouldBe invokeSingleFile()
  }

  @Test fun testComplexSpreadingSetup() = singleAndMultiCodegen(
    """
      typealias App = Any
  
      @Scoped<AppGivenScope>
      @Given
      class Dep(app: App)
  
      @Scoped<AppGivenScope>
      @Given
      class DepWrapper(dep: Dep)
  
      @Scoped<AppGivenScope>
      @Given
      class DepWrapper2(dep: () -> Dep, wrapper: () -> DepWrapper)
  
      @InstallElement<AppGivenScope>
      @Given
      class MyComponent(dep: Dep, wrapper: () -> () -> DepWrapper, wrapper2: () -> DepWrapper2)
  
      @Given
      fun myInitializer(dep: Dep, wrapper: () -> () -> DepWrapper, wrapper2: () -> DepWrapper2): GivenScopeInitializer<AppGivenScope> = {}
    """,
    """
      @GivenImports("com.ivianuu.injekt.common.*", "com.ivianuu.injekt.scope.*")
      fun invoke() {
          summon<(@Given @InstallElement<AppGivenScope> App) -> AppGivenScope>()
      }
    """
  )

  @Test fun testSpreadingGivenWithModuleLikeSpreadingReturnType() = singleAndMultiCodegen(
    """
      @Qualifier annotation class ClassSingleton
      
      @Given inline fun <@Spread T : @ClassSingleton U, reified U : Any> classSingleton(
        factory: () -> T
      ): U = factory()
  
      class MyModule<T : S, S> {
          @Given fun value(v: T): S = v
      }
  
      @Given fun <@Spread T : @Qualifier1 S, S> myModule():
          @ClassSingleton MyModule<T, S> = MyModule()
  
      @Given val foo: @Qualifier1 Foo = Foo()
    """,
    """
      fun invoke() = summon<Foo>() 
    """
  ) {
    irShouldContain(1, "classSingleton<@ClassSingleton MyModule")
  }

  @Test fun testSpreadingGivenWithModuleLikeSpreadingReturnType2() = singleAndMultiCodegen(
    """
      @Qualifier annotation class ClassSingleton
      
      @Given inline fun <@Spread T : @ClassSingleton U, reified U : Any> classSingleton(
        factory: () -> T,
        scope: AppGivenScope
      ): U = scope.getOrCreateScopedValue(U::class, factory)
      
      class MyModule<T : S, S> {
        @Given fun value(v: T): S = v
      }
      
      @Given fun <@Spread T : @Qualifier1 S, S> myModule(): 
        @ClassSingleton MyModule<T, S> = MyModule()
      
      @Given val foo: @Qualifier1 Foo = Foo()
    """,
    """
      @GivenImports("com.ivianuu.injekt.common.*", "com.ivianuu.injekt.scope.*")
      fun invoke() = summon<Set<Foo>>()
    """
  ) {
    irShouldContain(1, "setOf")
  }

  @Test fun testNestedSpreadingGivensWithGenerics() = singleAndMultiCodegen(
    """
      @Qualifier annotation class A<T>
      
      @Given class Outer<@Spread T : @A<U> S, S, U> {
        @Given fun <@Spread K : U> inner(): Unit = Unit
      }
      
      @Given fun dummy(): @A<String> Long = 0L
    """,
    """
      fun invoke() = summon<Unit>() 
    """
  ) {
    compilationShouldHaveFailed("no given argument found of type kotlin.Unit for parameter value of function com.ivianuu.injekt.summon")
  }

  @Test fun testSpreadingGivenWithInvariantTypeParameter() = singleAndMultiCodegen(
    """
      interface IntentKey
      
      typealias KeyIntentFactory<K> = (K) -> Any
      
      @Given fun <@Spread T : KeyIntentFactory<K>, K : IntentKey> impl() = Foo()
      
      class IntentKeyImpl : IntentKey
      
      @Given val keyIntentFactoryImpl: KeyIntentFactory<IntentKeyImpl> = { Any() }
    """,
    """
      fun invoke() = summon<Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }
}
