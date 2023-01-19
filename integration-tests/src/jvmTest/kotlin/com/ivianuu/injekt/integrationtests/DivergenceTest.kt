/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalCompilerApi::class)

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.singleAndMultiCodegen
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
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

  @Test fun testCanBreakCircularDependencyViaProvider() = singleAndMultiCodegen(
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
