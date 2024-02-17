
/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import io.kotest.matchers.types.*
import org.junit.*

class TagTest {
  @Test fun testDistinctTag() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide val taggedFoo: @Tag1 Foo = Foo()
    """,
    """
      fun invoke(): Pair<Foo, Foo> {
        return inject<Foo>() to inject<@Tag1 Foo>()
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
      fun invoke() = inject<Dep<String>>() 
    """
  )

  @Test fun testTaggedClass() = singleAndMultiCodegen(
    """ 
      @Provide @Tag1 class Dep
    """,
    """
      fun invoke() = inject<@Tag1 Dep>()
    """
  )

  @Test fun testTaggedPrimaryConstructor() = singleAndMultiCodegen(
    """ 
      class Dep @Provide @Tag1 constructor()
    """,
    """
      fun invoke() = inject<@Tag1 Dep>()
    """
  )

  @Test fun testTaggedSecondaryConstructor() = singleAndMultiCodegen(
    """ 
      class Dep {
        @Provide @Tag1 constructor()
      }
    """,
    """
      fun invoke() = inject<@Tag1 Dep>()
    """
  )

  @Test fun testTaggedObject() = singleAndMultiCodegen(
    """ 
      @Provide @Tag1 object Dep
    """,
    """
      fun invoke() = inject<@Tag1 Dep>()
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
      fun invoke(@Inject value: @MyTag String) {
      }
    """
  )

  @Test fun testTagWithTypeParameters() = singleAndMultiCodegen(
    """
      @Tag annotation class MyTag<T>
      @Provide val taggedFoo: @MyTag<String> Foo = Foo()
    """,
    """
      fun invoke() = inject<@MyTag<String> Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testTagWithGenericTypeArguments() = singleAndMultiCodegen(
    """
      @Tag annotation class MyTag<T>
      @Provide fun <T> taggedFoo(): @MyTag<T> Foo = Foo()
    """,
    """
      fun invoke() = inject<@MyTag<String> Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testTagTypeAliasPattern() = singleAndMultiCodegen(
    """
      @Tag annotation class TaggedFooTag
      typealias TaggedFoo = @TaggedFooTag Foo
      @Provide val taggedFoo: TaggedFoo = Foo()
    """,
    """
      fun invoke() = inject<TaggedFoo>()
    """
  )

  @Test fun testGenericTagTypeAliasPattern() = singleAndMultiCodegen(
    """
      typealias ComponentScope<N> = @ComponentScopeTag<N> String

      @Tag annotation class ComponentScopeTag<N> {
        @Provide companion object {
          @Provide fun <N> scope(): ComponentScope<N> = ""
        }
      }
    """,
    """
      fun invoke() = inject<ComponentScope<TestScope1>>()
    """
  )
}
