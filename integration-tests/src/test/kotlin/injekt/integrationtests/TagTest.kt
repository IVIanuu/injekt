
/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalCompilerApi::class)

package injekt.integrationtests

import io.kotest.matchers.types.*
import org.jetbrains.kotlin.compiler.plugin.*
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

  @Test fun testTaggedClass() = singleAndMultiCodegen(
    """ 
      @Provide @Tag1 class Baz
    """,
    """
      fun invoke() = inject<@Tag1 Baz>()
    """
  )

  @Test fun testTaggedPrimaryConstructor() = singleAndMultiCodegen(
    """ 
      class Baz @Provide @Tag1 constructor()
    """,
    """
      fun invoke() = inject<@Tag1 Baz>()
    """
  )

  @Test fun testTaggedSecondaryConstructor() = singleAndMultiCodegen(
    """ 
      class Baz {
        @Provide @Tag1 constructor()
      }
    """,
    """
      fun invoke() = inject<@Tag1 Baz>()
    """
  )

  @Test fun testTagWithValueParameters() = codegen(
    """ 
      @Tag annotation class MyTag(val value: String)
    """
  ) {
    compilationShouldHaveFailed("tag cannot have value parameters")
  }

  @Test fun testTagWithTypeParameters() = singleAndMultiCodegen(
    """
      @Tag @Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.TYPE)
      annotation class MyTag<T>
      @Provide val taggedFoo: @MyTag<String> Foo = Foo()
    """,
    """
      fun invoke() = inject<@MyTag<String> Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testTagTypeAliasPattern() = singleAndMultiCodegen(
    """
      @Tag @Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.TYPE)
      annotation class TaggedFooTag
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

      @Tag @Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.TYPE)
      annotation class ComponentScopeTag<N> {
        @Provide companion object {
          @Provide fun <N> scope(): ComponentScope<N> = ""
        }
      }
    """,
    """
      fun invoke() = inject<ComponentScope<Foo>>()
    """
  )
}
