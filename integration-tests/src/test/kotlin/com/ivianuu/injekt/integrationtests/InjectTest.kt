package com.ivianuu.injekt.integrationtests

import io.kotest.matchers.types.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.junit.*

@OptIn(ExperimentalCompilerApi::class) class InjectTest {
  @Test fun testInjectFunctionValueParameter() = codegen(
    """
      fun invoke(foo: Foo = inject) = inject<Foo>()
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testInjectConstructorValueParameter() = codegen(
    """
      class Baz(val foo: Foo = inject)
      fun invoke(foo: Foo = inject) = Baz().foo
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }
}
