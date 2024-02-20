package com.ivianuu.injekt.integrationtests

import io.kotest.matchers.nulls.*
import io.kotest.matchers.types.*
import org.junit.*

class ProvideTest {
  @Test fun testTopLevelInjectableProperty() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testTopLevelInjectableFunction() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)
    """,
    """
      fun invoke() = inject<Bar>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Bar>()
  }

  @Test fun testTopLevelInjectableClass() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide class Baz(val foo: Foo)
    """,
    """
      fun invoke() = inject<Baz>().foo
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testTopLevelInjectableObject() = singleAndMultiCodegen(
    """
      @Provide object Baz
    """,
    """
      fun invoke() = inject<Baz>()
    """
  ) {
    invokeSingleFile().shouldNotBeNull()
  }

  @Test fun testNestedInjectableClass() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      object Container {
        @Provide class Baz(val foo: Foo)
      }
    """,
    """
      fun invoke() = inject<Container.Baz>().foo
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectablePrimaryConstructor() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      class Baz @Provide constructor(val foo: Foo)
    """,
    """
      fun invoke() = inject<Baz>().foo
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableSecondaryConstructor() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      class Baz(val any: Any) {
        @Provide constructor(foo: Foo) : this(any = foo)
      }
    """,
    """
      fun invoke() = inject<Baz>().any
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }
}
