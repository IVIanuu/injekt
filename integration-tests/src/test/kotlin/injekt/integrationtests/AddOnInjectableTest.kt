/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalCompilerApi::class)

package injekt.integrationtests

import io.kotest.matchers.collections.*
import io.kotest.matchers.types.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.junit.*

class AddOnInjectableTest {
  @Test fun testAddOnInjectableFunction() = singleAndMultiCodegen(
    """
      @Provide fun <@AddOn T : @Tag1 S, S> addOn(instance: T): S = instance
      @Provide fun taggedFoo(): @Tag1 Foo = Foo()
    """,
    """
      fun invoke() = create<Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testAddOnInjectableClass() = singleAndMultiCodegen(
    """
      @Provide class AddOnModule<@AddOn T : @Tag1 S, S> {
        @Provide fun untagged(instance: T): S = instance
      }
      @Provide fun taggedFoo(): @Tag1 Foo = Foo()
    """,
    """
      fun invoke() = create<Foo>()
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testAddOnInjectableChain() = singleAndMultiCodegen(
    """
      @Provide fun <@AddOn T : @Tag1 S, S> tag1AddOn(instance: T): @Tag2 S = instance
      @Provide fun <@AddOn T : @Tag2 S, S> tag2AddOn(instance: T): S = instance
      
      @Provide fun taggedFoo(): @Tag1 Foo = Foo()
    """,
    """
      fun invoke() = create<Foo>()
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testMultipleAddOnInjectableResults() = singleAndMultiCodegen(
    """
      @Provide fun <@AddOn T : @Tag1 String> addOn(instance: T): String = instance
      @Provide fun a(): @Tag1 String = "a"
      @Provide fun b(): @Tag1 String = "b"
    """,
    """
      fun invoke() = create<List<String>>() 
    """
  ) {
    invokeSingleFile<Set<String>>()
      .shouldContainExactly("a", "b")
  }

  @Test fun testRecursiveAddOnInjectables() = singleAndMultiCodegen(
    """
      @Provide fun <@AddOn T : Foo> fooToBar(foo: T): Bar = Bar(foo)
      @Provide fun <@AddOn T : Bar> barToFoo(bar: T): Foo = bar.foo
      @Provide fun foo() = Foo()
    """,
    """
      fun invoke() = create<Bar>() 
    """
  )
}
