/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.*
import org.junit.*

class InjectableDeclarationCheckTest {
  @Test fun testProvideAnnotationClass() = codegen(
    """
      @Provide annotation class MyAnnotation
    """
  ) {
    compilationShouldHaveFailed("annotation class cannot be injectable")
  }

  @Test fun testProvideConstructorOnAnnotationClass() = codegen(
    """
      annotation class MyAnnotation @Provide constructor()
    """
  ) {
    compilationShouldHaveFailed("annotation class cannot be injectable")
  }

  @Test fun testProvideEnumClass() = codegen(
    """
      @Provide enum class MyEnum
    """
  ) {
    compilationShouldHaveFailed("enum class cannot be injectable")
  }

  @Test fun testProvideInnerClass() = codegen(
    """
      class MyOuterClass {
        @Provide inner class MyInnerClass
      }
    """
  ) {
    compilationShouldHaveFailed("inner class cannot be injectable")
  }

  @Test fun testProvideAbstractClass() = codegen(
    """
      @Provide abstract class MyClass
    """
  ) {
    compilationShouldHaveFailed("abstract class cannot be injectable")
  }

  @Test fun testProvideConstructorAbstractClass() = codegen(
    """
      abstract class MyClass @Provide constructor()
    """
  ) {
    compilationShouldHaveFailed("abstract class cannot be injectable")
  }

  @Test fun testProvideInterface() = codegen(
    """
      @Provide interface MyInterface
    """
  ) {
    compilationShouldHaveFailed("interface cannot be injectable")
  }

  @Test fun testInjectValueParameterOnProvideFunction() = codegen(
    """
      @Provide fun bar(@Inject foo: Foo) = Bar(foo)
    """
  ) {
    compilationShouldHaveFailed("parameters of a injectable are automatically treated as inject parameters")
  }

  @Test fun testInjectValueParameterOnProvideClass() = codegen(
    """
      @Provide class MyBar(@Inject foo: Foo)
    """
  ) {
    compilationShouldHaveFailed("parameters of a injectable are automatically treated as inject parameters")
  }

  @Test fun testProvideValueParameterOnProvideFunction() = codegen(
    """
      @Provide fun bar(@Provide foo: Foo) = Bar(foo)
    """
  ) {
    compilationShouldHaveFailed("parameters of a injectable are automatically provided")
  }

  @Test fun testProvideValueParameterPropertyOnProvideClass() = codegen(
    """
      @Provide class Dep(@Provide val foo: Foo)
    """
  )

  @Test fun testOverrideProvideValueParameterPropertyOnProvideClass() = codegen(
    """
      abstract class AbstractDep {
        @Provide abstract val foo: Foo
      }
      @Provide class Dep(@Provide override val foo: Foo) : AbstractDep()
    """
  )

  @Test fun testProvideValueParameterOnProvideClass() = codegen(
    """
      @Provide class MyBar(@Provide foo: Foo)
    """
  ) {
    compilationShouldHaveFailed("parameters of a injectable are automatically provided")
  }

  @Test fun testInjectReceiverOnFunction() = codegen(
    """
      fun @receiver:Inject Foo.bar() = Bar(this)
    """
  ) {
    compilationShouldHaveFailed("receiver cannot be injected")
  }

  @Test fun testInjectReceiverOnProperty() = codegen(
    """
      val @receiver:Provide Foo.bar get() = Bar(this)
    """
  ) {
    compilationShouldHaveFailed("receiver is automatically provided")
  }

  @Test fun testProvideLocalVariableWithoutInitializer() = codegen(
    """
      fun invoke() {
        @Provide val a: String
        inject<String>()
        a = ""
      }
    """
  ) {
    compilationShouldHaveFailed("injectable variable must be initialized, delegated or marked with lateinit")
  }

  @Test fun testMultipleInjectAnnotatedFunctionParameters() = codegen(
    """
      fun invoke(@Inject foo: Foo, @Inject bar: Bar) {
      }
    """
  ) {
    compilationShouldHaveFailed(
      "parameters after the first @Inject parameter are automatically treated as inject parameters"
    )
  }

  @Test fun testMultipleInjectAnnotatedConstructorParameters() = codegen(
    """
      class MyClass(@Inject foo: Foo, @Inject bar: Bar)
    """
  ) {
    compilationShouldHaveFailed(
      "parameters after the first @Inject parameter are automatically treated as inject parameters"
    )
  }
}
