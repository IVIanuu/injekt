/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.*
import io.kotest.matchers.*
import io.kotest.matchers.types.*
import org.junit.*

class ModuleTest {
  @Test fun testClassModule() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide class BarModule(private val foo: Foo) {
        @Provide val bar get() = Bar(foo)
      }
    """,
    """
      fun invoke() = inject<Bar>() 
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
      fun invoke() = inject<Bar>() 
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
      fun invoke() = withModule { inject<Bar>() }
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
        fun invoke() = inject<Pair<Foo, Foo>>() 
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
      fun invoke() = inject<@MyTag<Int> Pair<Foo, Foo>>() 
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
        inject<Pair<Foo, Foo>>()
        inject<Pair<Bar, Bar>>()
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
        inject<Pair<Foo, Foo>>()
        inject<Pair<Bar, Bar>>() 
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
      fun invoke() = inject<Bar>() 
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
      fun invoke() = inject<Dep>() 
    """
  )

  @Test fun testLambdaModule() = codegen(
    """
      fun invoke() = inject<(@Provide () -> Foo) -> Foo>()
    """
  ) {
    val foo = Foo()
    invokeSingleFile<(() -> Foo) -> Foo>()({ foo }) shouldBeSameInstanceAs foo
  }

  @Test fun testLambdaModuleChain() = singleAndMultiCodegen(
    """
      @Provide val fooModule: @Provide () -> @Provide () -> Foo = { { Foo() } }
    """,
    """
      fun invoke() = inject<Foo>() 
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
      fun invoke() = inject<List<Foo>>()
    """
  ) {
    val foos = invokeSingleFile<List<Foo>>()
    foos shouldBe foos.distinct()
  }
}
