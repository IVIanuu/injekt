/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import androidx.compose.runtime.*
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
      fun invoke(): @Composable () -> Unit = {
        testKeyUi()
      }
    """,
    config = { withCompose() }
  ) {
    runComposing {
      invokeSingleFile<@Composable () -> Unit>().invoke()
    }
  }

  @Test fun testComposeFunInterfaceWithFunctionSuperType2() = singleAndMultiCodegen(
    """
      fun interface KeyUi<K> : @Composable () -> Unit
    """,
    """
        data class Holder(val keyUi: KeyUi<*>)
        val keyUi = KeyUi<Any> {}
    """,
    """
      fun invoke(): @Composable () -> Unit = {
        Holder(keyUi)
      }
    """,
    config = { withCompose() }
  ) {
    runComposing {
      invokeSingleFile<@Composable () -> Unit>().invoke()
    }
  }

  @Test fun testComposeFunInterfaceWithFunctionSuperType3() = multiCodegen(
    """
      fun interface KeyUi<K> : @Composable () -> Unit
    """,
    """
        val keyUi: KeyUi<Any>? = KeyUi<Any> {}
    """,
    """
      fun invoke(): @Composable () -> Unit = {
        keyUi?.invoke()
      }
    """,
    config = { withCompose() }
  ) {
    runComposing {
      invokeSingleFile<@Composable () -> Unit>().invoke()
    }
  }

  @Test fun testComposableFunInterfaceWithComposableFunction() = singleAndMultiCodegen(
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
      fun invoke(): @Composable () -> Unit = {
        testKeyUi.invoke(
          object : ModelKeyUiScope<String, Int> {
            override val model: Int get() = 0
          }
        )
      }
    """,
    config = { withCompose() }
  ) {
    runComposing {
      invokeSingleFile<@Composable () -> Unit>().invoke()
    }
  }

  // todo @Test
  fun testComposableFunInterfaceWithComposableExtensionFunction() = singleAndMultiCodegen(
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
