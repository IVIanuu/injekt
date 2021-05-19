package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.*
import io.kotest.matchers.types.*
import org.junit.*

class OverrideDeclarationTest {
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
