/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.*
import org.junit.*

class ComposeFunInterfaceFixTest {
  @Test fun testComposeFunInterfaceWithFunctionSuperType() = singleAndMultiCodegen(
    """
      fun interface KeyUi<K> : @Composable () -> Unit
    """,
    """
      val testKeyUi = KeyUi<String> {
        val test = remember { "" }
      }
    """,
    """
      @Composable fun func() {
        testKeyUi()
      }
    """,
    config = { withCompose() }
  )

  @Test fun testComposableFunInterfaceWithComposableFunction() = multiCodegen(
    """
      fun interface ModelKeyUi<K, M> {
        @Composable operator fun invoke(scope: ModelKeyUiScope<K, M>)
      }

      interface ModelKeyUiScope<K, M> {
        val model: M
      }
    """,
    """
      val testKeyUi = ModelKeyUi<String, Int> {
        val test = remember { it.model }
      }
    """,
    """
      @Composable fun func() {
        testKeyUi.invoke(
          object : ModelKeyUiScope<String, Int> {
            override val model: Int get() = 0
          }
        )
      }
    """,
    config = { withCompose() }
  )

  // todo @Test
  fun testComposableFunInterfaceWithComposableExtensionFunction() = multiCodegen(
    """
      fun interface ModelKeyUi<K, M> {
        @Composable operator fun ModelKeyUiScope<K, M>.invoke()
      }

      interface ModelKeyUiScope<K, M> {
        val model: M
      }
    """,
    """
      val testKeyUi = ModelKeyUi<String, Int> {
        val test = remember { model }
      }
    """,
    """
      @Composable fun func() {
        with(
          object : ModelKeyUiScope<String, Int> {
            override val model: Int get() = 0
          }
        ) {
          with(testKeyUi) {
            invoke()
          }
        }
      }
    """,
    config = { withCompose() }
  )
}
