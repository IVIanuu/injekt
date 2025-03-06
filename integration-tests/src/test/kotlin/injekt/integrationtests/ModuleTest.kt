/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalCompilerApi::class)

package injekt.integrationtests

import io.kotest.matchers.*
import io.kotest.matchers.types.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.junit.*

class ModuleTest {
  @Test fun testClassModule() = singleAndMultiCodegen(
    """
      data class Baz(val foo: Foo, val bar: Bar)
      @Provide val foo = Foo()
      @Provide class BarModule {
        @Provide val bar = Bar(foo)
        @Provide fun baz(bar: Bar) = Baz(foo, bar)
      }
    """,
    """
      fun invoke() = inject<Baz>().bar.foo
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testSubClassModule() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      abstract class AbstractBarModule<T> {
        @Provide fun bar(foo: Foo, value: T) = Bar(foo)
      }
      @Provide class BarModuleImpl : AbstractBarModule<String>() {
        @Provide val string = ""
      } 
    """,
    """
      fun invoke() = inject<Bar>() 
    """
  )

  @Test fun testModuleWithTypeParameters() = singleAndMultiCodegen(
    """
      class MyModule<T>(private val instance: T) {
        @Provide fun provide() = instance to instance
      }
      @Provide fun fooModule() = MyModule(Foo())
      @Provide fun stringModule() = MyModule("__")
    """,
    """
        fun invoke() = inject<Pair<Foo, Foo>>() 
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

  @Test fun testLambdaModule() = codegen(
    """
      fun invoke() = inject<(@Provide () -> Foo) -> Foo>()
    """
  ) {
    val foo = Foo()
    invokeSingleFile<(() -> Foo) -> Foo>()({ foo }) shouldBeSameInstanceAs foo
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

  @Test fun testLambdaModuleWithFunctionSubType() = codegen(
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
    compilationShouldHaveFailed("injekt.integrationtests.fooModuleProvider.invoke.foo")
  }
}
