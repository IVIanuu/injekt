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

            @Given fun <T> unwrapped(@Given wrapped: Wrapper<T>): T = wrapped.value
        """,
    """
            fun invoke() {
                given<Foo>()
            }
        """
  ) {
    compilationShouldHaveFailed("diverging")
  }

  @Test fun testResolvableDivergence() = singleAndMultiCodegen(
    """
            interface Wrapper<T> {
                val value: T
            }

            @Given fun <T> unwrapped(@Given wrapped: Wrapper<T>): T = wrapped.value

            @Given fun fooWrapper(): Wrapper<Wrapper<Foo>> = error("")
        """,
    """
            fun invoke() {
                given<Foo>()
            } 
        """
  )

  @Test fun testCircularDependencyFails() = singleAndMultiCodegen(
    """
            @Given class A(@Given b: B)
            @Given class B(@Given a: A)
        """,
    """
           fun invoke() = given<A>() 
        """
  ) {
    compilationShouldHaveFailed("diverging")
  }

  @Test fun testProviderBreaksCircularDependency() = singleAndMultiCodegen(
    """
            @Given class A(@Given b: B)
            @Given class B(@Given a: () -> A)
       """,
    """
            fun invoke() = given<B>()
        """
  ) {
    invokeSingleFile()
  }

  @Test fun testIrrelevantProviderInChainDoesNotBreakCircularDependency() = singleAndMultiCodegen(
    """
            @Given class A(@Given b: () -> B)
            @Given class B(@Given b: C)
            @Given class C(@Given b: B)
       """,
    """
           fun invoke() = given<C>() 
        """
  ) {
    compilationShouldHaveFailed("diverging")
  }

  @Test fun testLazyRequestInSetBreaksCircularDependency() = singleAndMultiCodegen(
    """
            typealias A = () -> Unit
            @Given fun a(@Given b: () -> B): A = {}
            typealias B = () -> Unit
            @Given fun b(@Given a: () -> A): B = {}
       """,
    """
           fun invoke() = given<Set<() -> Unit>>() 
        """
  ) {
    invokeSingleFile()
  }
}