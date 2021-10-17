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

import com.ivianuu.injekt.test.singleAndMultiCodegen
import org.junit.Test

class ModuleTest {
  @Test fun testClassModule() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide class BarModule(private val foo: Foo) {
        @Provide val bar get() = Bar(foo)
      }
    """,
    """
      fun invoke() = inject<Bar>() 
    """
  )

  @Test fun testObjectModule() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide object BarModule {
        @Provide fun bar(foo: Foo) = Bar(foo)
      }
    """,
    """
      fun invoke() = inject<Bar>() 
    """
  )

  @Test fun testModuleLambdaParameter() = singleAndMultiCodegen(
    """
      class MyModule {
        @Provide val foo = Foo()
      }

      @Provide fun foo() = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)

      inline fun <R> withModule(
        block: (@Provide MyModule) -> R
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
        @Provide fun provide() = instance to instance
      }
      @Provide val fooModule = MyModule(Foo())
      @Provide val stringModule = MyModule("__")
    """,
    """
        fun invoke() = inject<Pair<Foo, Foo>>() 
    """
  )

  @Test fun testGenericModuleClass() = singleAndMultiCodegen(
    """
      @Provide class MyModule<T> {
        @Provide fun provide(instance: T) = instance to instance
      }
  
      @Provide val foo = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)
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
        @Provide fun provide(instance: T) = instance to instance
      }

      @Provide fun <T> myModule() = MyModule<T>()

      @Provide val foo = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)
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
      @Provide val foo = Foo()
      abstract class BaseBarModule(private val foo: Foo) {
        @Provide val bar get() = Bar(foo)
      }
      @Provide class BarModule(private val foo: Foo) : BaseBarModule(foo)
    """,
    """
      fun invoke() = inject<Bar>() 
    """
  )
}
