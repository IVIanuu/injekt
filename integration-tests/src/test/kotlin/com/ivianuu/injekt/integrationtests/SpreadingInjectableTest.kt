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

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.irShouldContain
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.shouldNotContainMessage
import com.ivianuu.injekt.test.singleAndMultiCodegen
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.Test

class SpreadingInjectableTest {
  @Test fun testSpreadingInjectableFunction() = singleAndMultiCodegen(
    """
      @Tag annotation class Trigger
      @Provide fun <@Spread T : @Trigger S, S> triggerImpl(instance: T): S = instance

      @Provide fun foo(): @Trigger Foo = Foo()
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testSpreadingInjectableClass() = singleAndMultiCodegen(
    """
      @Provide class MyModule<@Spread T : @Trigger S, S> {
          @Provide fun intoSet(instance: T): @Final S = instance
      }
      @Tag annotation class Trigger

      @Tag annotation class Final

      @Provide fun foo(): @Trigger Foo = Foo()
      @Provide fun string(): @Trigger String = ""
    """,
    """
      fun invoke() = inject<List<@Final Foo>>() 
    """
  ) {
    invokeSingleFile<Set<Foo>>().size shouldBe 1
  }

  @Test fun testSpreadingNonInjectableClass() = codegen(
    """
      class MyModule<@Spread T>
    """
  ) {
    compilationShouldHaveFailed("a @Spread type parameter is only supported on @Provide functions and @Provide classes")
  }

  @Test fun testSpreadingNonInjectableFunction() = codegen(
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

  @Test fun testSpreadingInjectableTriggeredByClass() = singleAndMultiCodegen(
    """
      @Tag annotation class Trigger
      @Provide fun <@Spread T : @Trigger S, S> triggerImpl(instance: T): S = instance
      
      @Trigger @Provide class NotAny
    """,
    """
      fun invoke() = inject<NotAny>() 
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testSpreadingInjectableChain() = singleAndMultiCodegen(
    """
      @Tag annotation class A
      
      @Provide fun <@Spread T : @A S, S> aImpl() = AModule_<S>()
      
      class AModule_<T> {
        @Provide
        fun my(instance: T): @B T = instance
      }
      
      @Tag annotation class B
      @Provide fun <@Spread T : @B S, S> bImpl() = BModule_<T>()
      
      class BModule_<T> {
        @Provide
        fun my(instance: T): @C Any? = instance
      }
      
      @Tag annotation class C
      @Provide fun <@Spread T : @C Any?> cImpl() = Foo()
      
      @Provide fun dummy(): @A Long = 0L
    """,
    """
      fun invoke() = inject<List<Foo>>().single() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testMultipleSpreadCandidatesWithSameType() = singleAndMultiCodegen(
    """
      @Tag annotation class Trigger
      @Provide fun <@Spread T : @Trigger String> triggerImpl(instance: T): String = instance

      @Provide fun a(): @Trigger String = "a"
      @Provide fun b(): @Trigger String = "b"
    """,
    """
      fun invoke() = inject<List<String>>() 
    """
  ) {
    invokeSingleFile<Set<String>>()
      .shouldContainExactly("a", "b")
  }

  @Test fun testSpreadTypeParameterIsNotMarkedAsUnused() = codegen(
    """
      @Tag annotation class Trigger
      @ProvideSetElement fun <@Spread T : @Trigger String> triggerImpl(): String = ""
    """
  ) {
    shouldNotContainMessage("Type parameter \"T\" is never used")
  }

  @Test fun testNoFinalTypeWarningOnSpreadTypeParameter() = codegen(
    """
      @Tag annotation class Trigger
      @ProvideSetElement fun <@Spread T : @Trigger String> triggerImpl(): String = ""
    """
  ) {
    shouldNotContainMessage("'String' is a final type, and thus a value of the type parameter is predetermined")
  }

  @Test fun testCanResolveTypeBasedOnSpreadConstraintType() = singleAndMultiCodegen(
    """
      @Tag annotation class Trigger
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

  @Test fun testUiDecorator() = multiCodegen(
    """
      typealias UiDecorator = @Composable (@Composable () -> Unit) -> Unit
  
      @Tag annotation class UiDecoratorBinding
  
      @Provide fun <@Spread T : @UiDecoratorBinding S, S : UiDecorator> uiDecoratorBindingImpl(
        instance: T,
        key: TypeKey<S>
      ): UiDecorator = instance as UiDecorator
  
      typealias RootSystemBarsProvider = UiDecorator
  
      @Provide fun rootSystemBarsProvider(): @UiDecoratorBinding RootSystemBarsProvider = {}
    """,
    """
      fun invoke() = inject<List<UiDecorator>>().size 
    """
  ) {
    1 shouldBe invokeSingleFile()
  }

  @Test fun testSpreadingInjectableWithModuleLikeSpreadingReturnType() = singleAndMultiCodegen(
    """
      @Tag annotation class SingleInstance
      
      @Provide inline fun <@Spread T : @SingleInstance U, reified U : Any> SingleInstance(
        factory: () -> T
      ): U = factory()
  
      class MyModule<T : S, S> {
        @Provide fun value(v: T): S = v
      }
  
      @Provide fun <@Spread T : @Tag1 S, S> myModule():
          @SingleInstance MyModule<T, S> = MyModule()
  
      @Provide val foo: @Tag1 Foo = Foo()
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    irShouldContain(1, "SingleInstance<@SingleInstance MyModule")
  }

  @Test fun testSpreadingInjectableWithModuleLikeSpreadingReturnType2() = singleAndMultiCodegen(
    """
      @Tag annotation class SingleInstance
      
      @Provide inline fun <@Spread T : @SingleInstance U, reified U : Any> SingleInstance(
        factory: () -> T
      ): U = factory()
      
      class MyModule<T : S, S> {
        @Provide fun value(v: T): S = v
      }
      
      @Provide fun <@Spread T : @Tag1 S, S> myModule(): 
        @SingleInstance MyModule<T, S> = MyModule()
      
      @Provide val foo: @Tag1 Foo = Foo()
    """,
    """
      fun invoke() = inject<List<Foo>>()
    """
  ) {
    irShouldContain(1, "setOf")
  }

  @Test fun testNestedSpreadingInjectablesWithGenerics() = singleAndMultiCodegen(
    """
      @Tag annotation class A<T>
      
      @Provide class Outer<@Spread T : @A<U> S, S, U> {
        @Provide fun <@Spread K : U> inner(): Unit = Unit
      }
      
      @Provide fun dummy(): @A<String> Long = 0L
    """,
    """
      fun invoke() = inject<Unit>() 
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type kotlin.Unit for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testSpreadingInjectableWithInvariantTypeParameter() = singleAndMultiCodegen(
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

  @Test fun testSpreadingInjectableWithComponent() = codegen(
    """
      @Tag annotation class Trigger
      @Provide fun <@Spread T : @Trigger S, S> triggerImpl(instance: T): S = instance

      @Component @Trigger interface FooComponent { 
        val foo: Foo
      }
  
      @Provide val foo = Foo()
  
      fun invoke() = inject<FooComponent>()
    """
  )
}
