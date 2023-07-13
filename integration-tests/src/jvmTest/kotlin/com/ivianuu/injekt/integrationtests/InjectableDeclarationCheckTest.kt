/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.Test

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
      @Provide class Dep(@property:Provide override val foo: Foo) : AbstractDep()
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

  @Test fun testProvideFunctionOverrideWithProvideAnnotation() = singleAndMultiCodegen(
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
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
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
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testProvideFunctionOverrideWithoutProvideAnnotation() = codegen(
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

  @Test fun testFunctionWithInjectParameterOverrideWithoutInjectAnnotation() = codegen(
    """
      abstract class MySuperClass {
        abstract fun bar(@Inject foo: Foo): Bar
      }
    """,
    """
      class MySubClass : MySuperClass() {
        override fun bar(foo: Foo) = Bar(foo)
      } 
    """
  ) {
    compilationShouldHaveFailed("'bar' overrides nothing")
  }

  @Test fun testNonSpreadTypeParameterOverrideWithSpreadOverridden() = singleAndMultiCodegen(
    """
      abstract class MySuperClass {
        @Provide abstract fun <@Spread T : Bar> foo(): Foo
      }
    """,
    """
      class MySubClass : MySuperClass() {
        @Provide override fun <T : Bar> foo(): Foo = TODO()
      } 
    """
  ) {
    compilationShouldHaveFailed("'foo' overrides nothing")
  }

  @Test fun testProvidePropertyOverrideWithoutProvideAnnotation() = singleAndMultiCodegen(
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

  @Test fun testActualProvideFunctionWithoutProvideAnnotation() = multiPlatformCodegen(
    """
      @Provide expect fun foo(): Foo 
    """,
    """
      actual fun foo(): Foo = Foo()
    """
  ) {
    compilationShouldHaveFailed("Actual function 'foo' has no corresponding expected declaration")
  }

  @Test fun testActualProvidePropertyWithoutProvideAnnotation() = multiPlatformCodegen(
    """
      @Provide expect val foo: Foo 
    """,
    """
      actual val foo: Foo = Foo()
    """
  ) {
    compilationShouldHaveFailed("Actual property 'foo' has no corresponding expected declaration")
  }

  @Test fun testActualProvideClassWithoutProvideAnnotation() = multiPlatformCodegen(
    """
      @Provide expect class Dep 
    """,
    """
      actual class Dep
    """
  ) {
    compilationShouldHaveFailed("Actual class 'Dep' has no corresponding expected declaration")
  }

  @Test fun testActualProvideConstructorWithoutProvideAnnotation() = multiPlatformCodegen(
    """
      expect class Dep {
        @Provide constructor()
      }
    """,
    """
      actual class Dep {
        actual constructor()
      }
    """
  ) {
    compilationShouldHaveFailed("Actual constructor of 'Dep' has no corresponding expected declaration")
  }

  @Test fun testExpectActualFunctionSpreadTypeParameterMismatch() = multiPlatformCodegen(
    """
      @Provide expect fun <@Spread T> myFunc(): Foo
    """,
    """
      @Provide actual fun <T> myFunc(): Foo = Foo()
    """
  ) {
    compilationShouldHaveFailed("Actual function 'myFunc' has no corresponding expected declaration")
  }

  @Test fun testExpectActualClassSpreadTypeParameterMismatch() = multiPlatformCodegen(
    """
      @Provide expect class Dep<@Spread T>
    """,
    """
      @Provide actual class Dep<T>
    """
  ) {
    compilationShouldHaveFailed("Actual class 'Dep' has no corresponding expected declaration")
  }
}
