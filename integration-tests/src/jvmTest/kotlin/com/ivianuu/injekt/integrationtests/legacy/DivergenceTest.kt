/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests.legacy

import com.ivianuu.injekt.integrationtests.*
import org.junit.*

class DivergenceTest {
  @Test fun testUnresolvableDivergence() = singleAndMultiCodegen(
    """
      interface Base

      interface Wrapper<T> : Base {
        val value: T
      }
  
      @Provide fun <T : Base> unwrapped(wrapped: Wrapper<T>): T = wrapped.value
    """,
    """
      fun invoke() = inject<Base>()
    """
  ) {
    compilationShouldHaveFailed("diverging")
  }

  @Test fun testResolvableDivergence() = singleAndMultiCodegen(
    """
      interface Base

      interface Wrapper<T> : Base {
        val value: T
      }
  
      @Provide fun <T : Base> unwrapped(wrapped: Wrapper<T>): T = wrapped.value
  
      @Provide fun baseWrapper(): Wrapper<Wrapper<Base>> = error("")
    """,
    """
      fun invoke() = inject<Base>()
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

  @Test fun testLambdaBreaksCircularDependency() = singleAndMultiCodegen(
    """
      @Provide class A(b: B)
      @Provide class B(a: (B) -> A) {
        val a = a(this)
      }
    """,
    """
      fun invoke() = inject<B>()
    """
  ) {
    invokeSingleFile()
  }
}
