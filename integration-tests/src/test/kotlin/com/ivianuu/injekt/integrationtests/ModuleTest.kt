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

class ModuleTest {
  @Test fun testClassModule() = singleAndMultiCodegen(
    """
      @Given val foo = Foo()
      @Given class BarModule(private val foo: Foo) {
          @Given val bar get() = Bar(foo)
      }
    """,
    """
      fun invoke() = inject<Bar>() 
    """
  )

  @Test fun testObjectModule() = singleAndMultiCodegen(
    """
      @Given val foo = Foo()
      @Given object BarModule {
          @Given fun bar(foo: Foo) = Bar(foo)
      }
    """,
    """
      fun invoke() = inject<Bar>() 
    """
  )

  @Test fun testModuleLambdaParameter() = singleAndMultiCodegen(
    """
      class MyModule {
          @Given val foo = Foo()
      }

      @Given fun foo() = Foo()
      @Given fun bar(foo: Foo) = Bar(foo)

      inline fun <R> withModule(
          block: (@Given MyModule) -> R
      ): R = block(MyModule())
    """,
    """
      fun invoke() = withModule { 
            inject<Bar>()
      } 
    """
  )

  @Test fun testGenericModule() = singleAndMultiCodegen(
    """
            class MyModule<T>(private val instance: T) {
                @Given fun provide() = instance to instance
            }
            @Given val fooModule = MyModule(Foo())
            @Given val stringModule = MyModule("__")
    """,
    """
        fun invoke() = inject<Pair<Foo, Foo>>() 
    """
  )

  @Test fun testGenericModuleQualified() = singleAndMultiCodegen(
    """
            @Qualifier annotation class MyQualifier<T>
            class MyModule<T>(private val instance: T) {
                @Given fun provide(): @MyQualifier<Int> Pair<T, T> = instance to instance
            }

            @Given val fooModule = MyModule(Foo())
            @Given val stringModule = MyModule("__")
            """,
    """
         fun invoke() = inject<@MyQualifier<Int> Pair<Foo, Foo>>() 
            """
  )

  @Test fun testGenericModuleClass() = singleAndMultiCodegen(
    """
            @Given class MyModule<T> {
                @Given fun provide(instance: T) = instance to instance
            }

            @Given val foo = Foo()
            @Given fun bar(foo: Foo) = Bar(foo)
    """,
    """
         fun invoke() {
                inject<Pair<Foo, Foo>>()
                inject<Pair<Bar, Bar>>()
            } 
    """
  )

  @Test fun testGenericModuleFunction() = singleAndMultiCodegen(
    """
      class MyModule<T> {
          @Given fun provide(instance: T) = instance to instance
      }

      @Given fun <T> myModule() = MyModule<T>()

      @Given val foo = Foo()
      @Given fun bar(foo: Foo) = Bar(foo)
    """,
    """
      fun invoke() {
        inject<Pair<Foo, Foo>>()
        inject<Pair<Bar, Bar>>() 
      } 
    """
  )

  @Test fun testSubClassModule() = singleAndMultiCodegen(
    """
      @Given val foo = Foo()
      abstract class BaseBarModule(private val foo: Foo) {
        @Given val bar get() = Bar(foo)
      }
      @Given class BarModule(private val foo: Foo) : BaseBarModule(foo)
    """,
    """
      fun invoke() = inject<Bar>() 
    """
  )
}
