package com.ivianuu.injekt.integrationtests

import io.kotest.matchers.nulls.*
import io.kotest.matchers.types.*
import org.junit.*

class ProviderTest {
  @Test fun testTopLevelPropertyProvider() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testTopLevelFunctionProvider() = singleAndMultiCodegen(
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

  @Test fun testTopLevelClassProvider() = singleAndMultiCodegen(
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

  @Test fun testTopLevelObjectProvider() = singleAndMultiCodegen(
    """
      @Provide object Baz
    """,
    """
      fun invoke() = inject<Baz>()
    """
  ) {
    invokeSingleFile().shouldNotBeNull()
  }

  @Test fun testNestedClassProvider() = singleAndMultiCodegen(
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

  @Test fun testPrimaryConstructorProvider() = singleAndMultiCodegen(
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

  @Test fun testSecondaryConstructorProvider() = singleAndMultiCodegen(
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

  @Test fun testValueParameterProvider() = codegen(
    """
      @JvmOverloads fun invoke(@Provide foo: Foo = Foo()) = inject<Foo>()
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testLocalVariableProvider() = codegen(
    """
      fun invoke(): Foo {
        @Provide val foo = Foo()
        return inject()
      }
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }
}
