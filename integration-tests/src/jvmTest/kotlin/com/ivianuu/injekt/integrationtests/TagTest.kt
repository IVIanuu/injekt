/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.singleAndMultiCodegen
import io.kotest.matchers.types.shouldBeTypeOf
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import org.junit.Test

class TagTest {
  @Test fun testDistinctTag() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide val taggedFoo: @Tag1 Foo = Foo()
    """,
    """
      fun invoke(): Pair<Foo, Foo> {
        return context<Foo>() to context<@Tag1 Foo>()
      } 
    """
  ) {
    val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
    foo1 shouldNotBeSameInstanceAs foo2
  }

  @Test fun testTypeParameterWithTagUpperBound() = singleAndMultiCodegen(
    """
      @Provide class Dep<T>(val value: @Tag1 T)
            
      @Provide fun tagged(): @Tag1 String = ""
    """,
    """
      fun invoke() = context<Dep<String>>() 
    """
  )

  @Test fun testTaggedClass() = singleAndMultiCodegen(
    """ 
      @Provide @Tag1 class Dep
    """,
    """
      fun invoke() = context<@Tag1 Dep>()
    """
  )

  @Test fun testTaggedPrimaryConstructor() = singleAndMultiCodegen(
    """ 
      class Dep @Provide @Tag1 constructor()
    """,
    """
      fun invoke() = context<@Tag1 Dep>()
    """
  )

  @Test fun testTaggedSecondaryConstructor() = singleAndMultiCodegen(
    """ 
      class Dep {
        @Provide @Tag1 constructor()
      }
    """,
    """
      fun invoke() = context<@Tag1 Dep>()
    """
  )

  @Test fun testTaggedObject() = singleAndMultiCodegen(
    """ 
      @Provide @Tag1 object Dep
    """,
    """
      fun invoke() = context<@Tag1 Dep>()
    """
  )

  @Test fun testTagWithValueParameters() = codegen(
    """ 
      @Tag annotation class MyTag(val value: String)
    """
  ) {
    compilationShouldHaveFailed("tag cannot have value parameters")
  }

  @Test fun testTagDoesNotNeedToSpecifyTypeTarget() = singleAndMultiCodegen(
    """
      @Tag annotation class MyTag 
    """,
    """
      fun invoke(@Context value: @MyTag String) {
      }
    """
  )

  @Test fun testTagWithTypeParameters() = singleAndMultiCodegen(
    """
      @Tag annotation class MyTag<T>
      @Provide val taggedFoo: @MyTag<String> Foo = Foo()
    """,
    """
      fun invoke() = context<@MyTag<String> Foo>() 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testTagWithGenericTypeArguments() = singleAndMultiCodegen(
    """
      @Tag annotation class MyTag<T>
      @Provide fun <T> taggedFoo(): @MyTag<T> Foo = Foo()
    """,
    """
      fun invoke() = context<@MyTag<String> Foo>() 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testUiState() = singleAndMultiCodegen(
    """
      @Tag annotation class UiState

      @Provide fun <T> uiState(instance: @UiState T): T = instance

      @Provide val foo: @UiState Foo = Foo()
    """,
    """
      fun invoke() = context<Foo>() 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testTagTypeAliasPattern() = singleAndMultiCodegen(
    """
      @Tag annotation class TaggedFooTag
      typealias TaggedFoo = @TaggedFooTag Foo
      @Provide val taggedFoo: TaggedFoo = Foo()
    """,
    """
      fun invoke() = context<TaggedFoo>()
    """
  )

  @Test fun testGenericTagTypeAliasPattern() = singleAndMultiCodegen(
    """
      typealias ComponentScope<N> = @ComponentScopeTag<N> String

      @Tag annotation class ComponentScopeTag<N> {
        companion object {
          @Provide fun <N> scope(): ComponentScope<N> = ""
        }
      }
    """,
    """
      fun invoke() = context<ComponentScope<TestScope1>>()
    """
  )
}
