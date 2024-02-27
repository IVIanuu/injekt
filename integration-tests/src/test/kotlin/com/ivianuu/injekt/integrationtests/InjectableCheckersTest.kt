/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalCompilerApi::class)

package com.ivianuu.injekt.integrationtests

import io.kotest.matchers.types.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.junit.*

class InjectableCheckersTest {
  @Test fun testInjectableEnumClass() = codegen(
    """
      @Provide enum class MyEnum
    """
  ) {
    compilationShouldHaveFailed("enum class cannot be injectable")
  }

  @Test fun testInjectableAbstractClass() = codegen(
    """
      @Provide abstract class MyClass
    """
  ) {
    compilationShouldHaveFailed("injectable cannot be abstract")
  }

  @Test fun testInjectableInterface() = codegen(
    """
      @Provide interface MyInterface
    """
  ) {
    compilationShouldHaveFailed("injectable cannot be abstract")
  }

  @Test fun testInjectableFunctionOverrideWithProvideAnnotation() = singleAndMultiCodegen(
    """
      abstract class MySuperClass {
        @Provide abstract fun foo(): Foo
      }
    """,
    """
      @Provide class MySubClass : MySuperClass() {
        @Provide override fun foo() = Foo()
      }

      fun invoke() = inject<Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testFunctionOverrideWithProvideAnnotation() = singleAndMultiCodegen(
    """
      abstract class MySuperClass {
        abstract fun foo(): Foo
      }
  
      @Provide class MySubClass : MySuperClass() {
        @Provide override fun foo() = Foo()
      }
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableFunctionOverrideWithoutProvideAnnotation() = codegen(
    """
      abstract class MySuperClass {
        @Provide abstract fun foo(): Foo
      }
    """,
    """
      class MySubClass : MySuperClass() {
        override fun foo() = Foo()
      } 
    """
  ) {
    compilationShouldHaveFailed("'foo' overrides nothing")
  }

  @Test fun testNonAddOnTypeParameterOverrideWithAddOnOverridden() = singleAndMultiCodegen(
    """
      abstract class MySuperClass {
        @Provide abstract fun <@AddOn T : Bar> foo(): Foo
      }
    """,
    """
      class MySubClass : MySuperClass() {
        @Provide override fun <T : Bar> foo() = Foo()
      } 
    """
  ) {
    compilationShouldHaveFailed("'foo' overrides nothing")
  }

  @Test fun testInjectablePropertyOverrideWithoutProvideAnnotation() = singleAndMultiCodegen(
    """
      abstract class MySuperClass {
        @Provide abstract val foo: Foo
      }
    """,
    """
      class MySubClass : MySuperClass() {
        override val foo = Foo()
      } 
    """
  ) {
    compilationShouldHaveFailed("'foo' overrides nothing")
  }

  @Test fun testMultipleAddOnTypeParameters() = codegen(
    """
      @Provide fun <@AddOn T, @AddOn S> triggerImpl() = Unit
    """
  ) {
    compilationShouldHaveFailed("only one @AddOn")
  }
}
