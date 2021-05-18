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

class DivergenceTest {
  @Test fun testUnresolvableDivergence() = singleAndMultiCodegen(
    """
      interface Wrapper<T> {
        val value: T
      }
  
      @Provide fun <T> unwrapped(wrapped: Wrapper<T>): T = wrapped.value
    """,
    """
      fun invoke() = inject<Foo>()
    """
  ) {
    compilationShouldHaveFailed("diverging")
  }

  @Test fun testResolvableDivergence() = singleAndMultiCodegen(
    """
      interface Wrapper<T> {
        val value: T
      }
  
      @Given fun <T> unwrapped(wrapped: Wrapper<T>): T = wrapped.value
  
      @Given fun fooWrapper(): Wrapper<Wrapper<Foo>> = error("")
    """,
    """
      fun invoke() = inject<Foo>()
    """
  )

  @Test fun testCircularDependencyFails() = singleAndMultiCodegen(
    """
      @Given class A(b: B)
      @Given class B(a: A)
    """,
    """
      fun invoke() = inject<A>() 
    """
  ) {
    compilationShouldHaveFailed("diverging")
  }

  @Test fun testProviderBreaksCircularDependency() = singleAndMultiCodegen(
    """
      @Given class A(b: B)
      @Given class B(a: () -> A)
    """,
    """
      fun invoke() = inject<B>()
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testIrrelevantProviderInChainDoesNotBreakCircularDependency() = singleAndMultiCodegen(
    """
      @Given class A(b: () -> B)
      @Given class B(b: C)
      @Given class C(b: B)
     """,
     """
      fun invoke() = inject<C>() 
     """
  ) {
    compilationShouldHaveFailed("diverging")
  }

  @Test fun testLazyRequestInSetBreaksCircularDependency() = singleAndMultiCodegen(
    """
      typealias A = () -> Unit
      @Given fun a(b: () -> B): A = {}
      typealias B = () -> Unit
      @Given fun b(a: () -> A): B = {}
     """,
    """
     fun invoke() = inject<Set<() -> Unit>>() 
    """
  ) {
    invokeSingleFile()
  }
}