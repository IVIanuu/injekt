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

import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.singleAndMultiCodegen
import org.junit.Test

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
  
      @Provide fun <T> unwrapped(wrapped: Wrapper<T>): T = wrapped.value
  
      @Provide fun fooWrapper(): Wrapper<Wrapper<Foo>> = error("")
    """,
    """
      fun invoke() = inject<Foo>()
    """
  )

  @Test fun testCircularDependencyFails() = singleAndMultiCodegen(
    """
      @Provide class A(b: B)
      @Provide class B(a: A)
    """,
    """
      fun invoke() = inject<A>() 
    """
  ) {
    compilationShouldHaveFailed("diverging")
  }

  @Test fun testProviderBreaksCircularDependency() = singleAndMultiCodegen(
    """
      @Provide class A(b: B)
      @Provide class B(a: () -> A)
    """,
    """
      fun invoke() = inject<B>()
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testIrrelevantProviderInChainDoesNotBreakCircularDependency() = singleAndMultiCodegen(
    """
      @Provide class A(b: () -> B)
      @Provide class B(b: C)
      @Provide class C(b: B)
     """,
     """
       fun invoke() = inject<C>() 
     """
  ) {
    compilationShouldHaveFailed("diverging")
  }

  @Test fun testInlineProviderDoesNotBreakCircularDependency() = singleAndMultiCodegen(
    """
      @Provide class A(b: B)
      class B(a: A) {
        companion object {
          @Provide inline fun newInstance(a: () -> A) = B(a())
        }
      }
    """,
    """
      fun invoke() = inject<B>()
    """
  ) {
    compilationShouldHaveFailed("diverging")
  }

  @Test fun testNonInlineProviderInsideAChainBreaksCircularDependencyWithInlineProvider() = singleAndMultiCodegen(
    """
      @Provide class A(b: C)
      @Provide class B(a: () -> A)
      class C(b: B) {
        companion object {
          @Provide inline fun newInstance(b: () -> B) = C(b())
        }
      }
    """,
    """
      fun invoke() = inject<C>()
    """
  ) {
    invokeSingleFile()
  }
}
