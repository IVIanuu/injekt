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

class GivenDeclarationCheckTest {
  @Test fun testGivenAnnotationClass() = codegen(
    """
      @Provide annotation class MyAnnotation
    """
  ) {
    compilationShouldHaveFailed("annotation class cannot be @Provide")
  }

  @Test fun testGivenConstructorOnAnnotationClass() = codegen(
    """
      annotation class MyAnnotation @Provide constructor()
    """
  ) {
    compilationShouldHaveFailed("annotation class cannot be @Provide")
  }

  @Test fun testGivenEnumClass() = codegen(
    """
      @Provide enum class MyEnum
    """
  ) {
    compilationShouldHaveFailed("enum class cannot be @Provide")
  }

  @Test fun testGivenInnerClass() = codegen(
    """
      class MyOuterClass {
        @Provide inner class MyInnerClass
      }
    """
  ) {
    compilationShouldHaveFailed("@Provide class cannot be inner")
  }

  @Test fun testGivenAbstractClass() = codegen(
    """
      @Provide abstract class MyClass
    """
  ) {
    compilationShouldHaveFailed("@Provide class cannot be abstract")
  }

  @Test fun testGivenConstructorAbstractClass() = codegen(
    """
      abstract class MyClass @Provide constructor()
    """
  ) {
    compilationShouldHaveFailed("@Provide class cannot be abstract")
  }

  @Test fun testGivenInterface() = codegen(
    """
      @Provide interface MyInterface
    """
  ) {
    compilationShouldHaveFailed("interface cannot be @Provide")
  }

  @Test fun testNonGivenValueParameterOnGivenFunction() = codegen(
    """
      @Provide fun bar(@Provide foo: Foo) = Bar(foo)
    """
  ) {
    compilationShouldHaveFailed("parameters of a @Provide declaration are automatically treated as @Provide")
  }

  @Test fun testGivenValueParameterOnGivenClass() = codegen(
    """
      @Provide class MyBar(@Provide foo: Foo)
    """
  ) {
    compilationShouldHaveFailed("parameters of a @Provide declaration are automatically treated as @Provide")
  }

  @Test fun testGivenReceiverOnFunction() = codegen(
    """
      fun @receiver:Given Foo.bar() = Bar(this)
    """
  ) {
    compilationShouldHaveFailed("receiver cannot be marked as @Provide because it is implicitly @Provide")
  }

  @Test fun testGivenReceiverOnNonGivenFunction() = codegen(
    """
      val @receiver:Given Foo.bar get() = Bar(this)
    """
  ) {
    compilationShouldHaveFailed("receiver cannot be marked as @Provide because it is implicitly @Provide")
  }

  @Test fun testGivenFunctionOverrideWithGivenAnnotation() = singleAndMultiCodegen(
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

  @Test fun testFunctionOverrideWithGivenAnnotation() = singleAndMultiCodegen(
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

  @Test fun testGivenFunctionOverrideWithoutGivenAnnotation() = codegen(
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

  @Test fun testGivenPropertyOverrideWithoutGivenAnnotation() = singleAndMultiCodegen(
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

  @Test fun testActualGivenFunctionWithoutGivenAnnotation() = multiPlatformCodegen(
    """
      @Provide expect fun foo(): Foo 
    """,
    """
      actual fun foo(): Foo = Foo()
    """
  ) {
    compilationShouldHaveFailed("Actual function 'foo' has no corresponding expected declaration")
  }

  @Test fun testActualGivenPropertyWithoutGivenAnnotation() = multiPlatformCodegen(
    """
      @Provide expect val foo: Foo 
    """,
    """
      actual val foo: Foo = Foo()
    """
  ) {
    compilationShouldHaveFailed("Actual property 'foo' has no corresponding expected declaration")
  }

  @Test fun testActualGivenClassWithoutGivenAnnotation() = multiPlatformCodegen(
    """
      @Provide expect class Dep 
    """,
    """
      actual class Dep
    """
  ) {
    compilationShouldHaveFailed("Actual class 'Dep' has no corresponding expected declaration")
  }

  @Test fun testActualGivenConstructorWithoutGivenAnnotation() = multiPlatformCodegen(
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
