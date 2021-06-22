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

class StatelessInjectableTest {
  @Test fun testOptimizesInjectableClass() = singleAndMultiCodegen(
    """
      @Provide class MyModule {
        @Provide fun foo() = Foo()
      }
    """,
    """
      fun invoke() = inject<MyModule>()
    """
  ) {
    invokeSingleFile()
      .shouldBeSameInstanceAs(invokeSingleFile())
  }

  @Test fun testDoesNotOptimizeNormalClass() = singleAndMultiCodegen(
    """
      class MyModule
      @Provide val foo = Foo()
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    irShouldNotContain("INSTANCE")
    invokeSingleFile()
  }

  @Test fun testDoesNotOptimizeObject() = singleAndMultiCodegen(
    """
      @Provide object MyModule {
        @Provide val foo = Foo()
      }
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    irShouldNotContain("INSTANCE")
    invokeSingleFile()
  }

  @Test fun testDoesNotOptimizeInjectableWithConstructorParameters() = singleAndMultiCodegen(
    """
      @Provide class MyModule(val foo: Foo)
      @Provide val foo = Foo()
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    irShouldNotContain("INSTANCE")
    invokeSingleFile()
  }

  @Test fun testDoesNotOptimizeInjectableWithFields() = singleAndMultiCodegen(
    """
      @Provide class MyModule {
        @Provide val foo = Foo()
      }
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    irShouldNotContain("INSTANCE")
    invokeSingleFile()
  }

  @Test fun testDoesNotOptimizeInjectableWithInnerClass() = singleAndMultiCodegen(
    """
      @Provide class MyModule {
        inner class Inner
      }
      @Provide val foo = Foo()
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    irShouldNotContain("INSTANCE")
    invokeSingleFile()
  }

  @Test fun testDoesOptimizeInjectableWithComputedProperties() = singleAndMultiCodegen(
    """
      @Provide class MyModule {
        @Provide val foo get() = Foo()
      }
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    irShouldContain(if (!it) 2 else 1, "INSTANCE")
    invokeSingleFile()
  }
}
