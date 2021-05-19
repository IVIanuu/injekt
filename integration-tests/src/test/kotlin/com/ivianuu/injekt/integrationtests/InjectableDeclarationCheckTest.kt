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
}
