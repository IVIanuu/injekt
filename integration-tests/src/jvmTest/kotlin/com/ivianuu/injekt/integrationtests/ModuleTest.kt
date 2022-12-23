/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import androidx.compose.runtime.Composable
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.runComposing
import com.ivianuu.injekt.test.singleAndMultiCodegen
import com.ivianuu.injekt.test.withCompose
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ModuleTest {
  @Test fun testClassModule() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide class BarModule(private val foo: Foo) {
        @Provide val bar get() = Bar(foo)
      }
    """,
    """
      fun invoke() = context<Bar>() 
    """
  )

  @Test fun testObjectModule() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide object BarModule {
        @Provide fun bar(foo: Foo) = Bar(foo)
      }
    """,
    """
      fun invoke() = context<Bar>() 
    """
  )

  @Test fun testModuleLambdaParameter() = singleAndMultiCodegen(
    """
      class MyModule {
        @Provide val foo = Foo()
      }

      @Provide fun foo() = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)

      inline fun <R> withModule(block: (MyModule) -> R): R = block(MyModule())
    """,
    """
      fun invoke() = withModule { context<Bar>() }
    """
  )

  @Test fun testGenericModule() = singleAndMultiCodegen(
    """
      class MyModule<T>(private val instance: T) {
        @Provide fun provide() = instance to instance
      }
      @Provide val fooModule = MyModule(Foo())
      @Provide val stringModule = MyModule("__")
    """,
    """
        fun invoke() = context<Pair<Foo, Foo>>() 
    """
  )

  @Test fun testGenericModuleTagged() = singleAndMultiCodegen(
    """
      @Tag annotation class MyTag<T>
      class MyModule<T>(private val instance: T) {
        @Provide fun provide(): @MyTag<Int> Pair<T, T> = instance to instance
      }
  
      @Provide val fooModule = MyModule(Foo())
      @Provide val stringModule = MyModule("__")
    """,
    """
      fun invoke() = context<@MyTag<Int> Pair<Foo, Foo>>() 
    """
  )

  @Test fun testGenericModuleClass() = singleAndMultiCodegen(
    """
      @Provide class MyModule<T> {
        @Provide fun provide(instance: T) = instance to instance
      }
  
      @Provide val foo = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)
    """,
    """
      fun invoke() {
        context<Pair<Foo, Foo>>()
        context<Pair<Bar, Bar>>()
      } 
    """
  )

  @Test fun testGenericModuleFunction() = singleAndMultiCodegen(
    """
      class MyModule<T> {
        @Provide fun provide(instance: T) = instance to instance
      }

      @Provide fun <T> myModule() = MyModule<T>()

      @Provide val foo = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)
    """,
    """
      fun invoke() {
        context<Pair<Foo, Foo>>()
        context<Pair<Bar, Bar>>() 
      } 
    """
  )

  @Test fun testSubClassModule() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      abstract class BaseBarModule(private val foo: Foo) {
        @Provide val bar get() = Bar(foo)
      }
      @Provide class BarModule(private val foo: Foo) : BaseBarModule(foo)
    """,
    """
      fun invoke() = context<Bar>() 
    """
  )

  @Test fun testModuleWithNestedClass() = singleAndMultiCodegen(
    """
      interface Dep

      @Provide class DepModule {
        @Provide class DepImpl : Dep
      }
    """,
    """
      fun invoke() = context<Dep>() 
    """
  )

  @Test fun testLambdaModule() = codegen(
    """
      fun invoke() = context<(@Provide () -> Foo) -> Foo>()
    """
  ) {
    val foo = Foo()
    invokeSingleFile<(() -> Foo) -> Foo>()({ foo }) shouldBeSameInstanceAs foo
  }

  @Test fun testSuspendLambdaModule() = codegen(
    """
      fun invoke() = context<suspend (@Provide suspend () -> Foo) -> Foo>()
    """
  ) {
    runBlocking {
      val foo = Foo()
      invokeSingleFile<suspend (suspend () -> Foo) -> Foo>()({ foo }) shouldBeSameInstanceAs foo
    }
  }

  @Test fun testComposableLambdaModule() = codegen(
    """
      fun invoke() = context<@Composable (@Provide @Composable () -> Foo) -> Foo>()
    """,
    config = { withCompose() }
  ) {
    runComposing {
      val foo = Foo()
      invokeSingleFile<@Composable (@Composable () -> Foo) -> Foo>()({ foo }) shouldBeSameInstanceAs foo
    }
  }

  @Test fun testLambdaModuleChain() = singleAndMultiCodegen(
    """
      @Provide val fooModule: @Provide () -> @Provide () -> Foo = { { Foo() } }
    """,
    """
      fun invoke() = context<Foo>() 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testLambdaModuleIdentity() = codegen(
    """
      private val foo1 = Foo()
      @Provide val foo1Lambda: @Provide () -> Foo = { foo1 }
      private val foo2 = Foo()
      @Provide val foo2Lambda: @Provide () -> Foo = { foo2 }
      fun invoke() = context<List<Foo>>()
    """
  ) {
    val foos = invokeSingleFile<List<Foo>>()
    foos shouldBe foos.distinct()
  }
}
