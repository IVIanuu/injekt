/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldBeTypeOf
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
      fun invoke() = inject<Bar>() 
    """
  )

  @Test fun testNullableModule() = singleAndMultiCodegen(
    """
      class FooModule {
        @Provide val foo = Foo()
      }

      @Provide val nullableModule: FooModule? = FooModule()
    """,
    """
      fun invoke() = inject<Foo?>()
    """
  ) {
    invokeSingleFile()
  }

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

  @Test fun testModuleIdentity() = codegen(
    """
      class FooModule {
        @Provide val foo = Foo()
      }
      @Provide val fooModule1 = FooModule()
      @Provide val fooModule2 = FooModule()
      fun invoke() = inject<List<Foo>>()
    """
  ) {
    val foos = invokeSingleFile<List<Foo>>()
    foos shouldBe foos.distinct()
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

  @Test fun testLambdaModuleKFunction() = codegen(
    """
      @Provide val foo = Foo()
      fun createBar(foo: Foo) = Bar(foo)
      @Provide val barProvider: @Provide KFunction1<Foo, Bar> = ::createBar
      fun invoke() = inject<Bar>()
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testModuleChainErrorNames() = singleAndMultiCodegen(
    """
      class FooModule {
        @Provide fun foo(unit: Unit): Foo = Foo()
      }

      @Provide val fooModuleProvider: @Provide () -> FooModule = { FooModule() }
    """,
    """
      fun invoke() = inject<Foo>()
    """
  ) {
    compilationShouldHaveFailed("com.ivianuu.injekt.integrationtests.fooModuleProvider.invoke.foo")
  }

  @Test fun testModuleWithSpreadingInjectable() = codegen(
    """
      class Token

      class SpreadModule<T> {
        @Provide fun <@Spread S : T> token() = Token()
      }
      @Provide val fooModule1 = SpreadModule<Foo>()
      @Provide val fooModule2 = SpreadModule<Foo>()

      @Provide val foo = Foo()

      fun invoke() = inject<List<Token>>()
    """
  ) {
    val values = invokeSingleFile<List<Any>>()
    values shouldBe values.distinct()
  }
}
