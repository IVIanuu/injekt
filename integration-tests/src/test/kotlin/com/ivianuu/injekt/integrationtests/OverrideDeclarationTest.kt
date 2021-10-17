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

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiPlatformCodegen
import com.ivianuu.injekt.test.singleAndMultiCodegen
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.Test

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
