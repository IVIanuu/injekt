/*
 * Copyright 2021 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.*
import io.kotest.matchers.types.*
import org.junit.*

class InjectableDeclarationCheckTest {
  @Test fun testProvideAnnotationClass() = codegen(
    """
      @Provide annotation class MyAnnotation
    """
  ) {
    compilationShouldHaveFailed("annotation class cannot be provided")
  }

  @Test fun testProvideConstructorOnAnnotationClass() = codegen(
    """
      annotation class MyAnnotation @Provide constructor()
    """
  ) {
    compilationShouldHaveFailed("annotation class cannot be provided")
  }

  @Test fun testProvideEnumClass() = codegen(
    """
      @Provide enum class MyEnum
    """
  ) {
    compilationShouldHaveFailed("enum class cannot be provided")
  }

  @Test fun testProvideInnerClass() = codegen(
    """
      class MyOuterClass {
        @Provide inner class MyInnerClass
      }
    """
  ) {
    compilationShouldHaveFailed("inner class cannot be provided")
  }

  @Test fun testProvideAbstractClass() = codegen(
    """
      @Provide abstract class MyClass
    """
  ) {
    compilationShouldHaveFailed("abstract class cannot be provided")
  }

  @Test fun testProvideConstructorAbstractClass() = codegen(
    """
      abstract class MyClass @Provide constructor()
    """
  ) {
    compilationShouldHaveFailed("abstract class cannot be provided")
  }

  @Test fun testProvideInterface() = codegen(
    """
      @Provide interface MyInterface
    """
  ) {
    compilationShouldHaveFailed("interface cannot be provided")
  }

  @Test fun testInjectValueParameterOnProvideFunction() = codegen(
    """
      @Provide fun bar(@Inject foo: Foo) = Bar(foo)
    """
  ) {
    compilationShouldHaveFailed("parameters of a provide declaration are automatically treated as inject parameters")
  }

  @Test fun testInjectValueParameterOnProvideClass() = codegen(
    """
      @Provide class MyBar(@Inject foo: Foo)
    """
  ) {
    compilationShouldHaveFailed("parameters of a provide declaration are automatically treated as inject parameters")
  }

  @Test fun testInjectReceiverOnFunction() = codegen(
    """
      fun @receiver:Inject Foo.bar() = Bar(this)
    """
  ) {
    compilationShouldHaveFailed("receiver cannot be marked as @Provide because it is implicitly @Provide")
  }

  @Test fun testInjectReceiverOnProperty() = codegen(
    """
      val @receiver:Provide Foo.bar get() = Bar(this)
    """
  ) {
    compilationShouldHaveFailed("receiver cannot be marked as @Provide because it is implicitly @Provide")
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
    compilationShouldHaveFailed("Conflicting overloads")
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
}
