/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import androidx.compose.runtime.*
import com.ivianuu.injekt.test.*
import org.junit.*

class ComposeFunInterfaceFixTest {
  @Test fun testComposeFunInterfaceWithFunctionSuperType() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
          fun interface KeyUi<K> : @Composable () -> Unit
        """
        )
      ),
      listOf(
        source(
          """
          val testKeyUi = KeyUi<String> {
            val test = remember { "" }
          }
        """
        )
      ),
      listOf(
        invokableSource(
          """
          fun invoke(): @Composable () -> Unit = {
            testKeyUi()
          }
        """
        )
      )
    ),
    config = { withCompose() }
  ) {
    runComposing {
      invokeSingleFile<@Composable () -> Unit>().invoke()
    }
  }

  @Test fun testComposeFunInterfaceWithFunctionSuperType2() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            fun interface KeyUi<K> : @Composable () -> Unit
          """
        )
      ),
      listOf(
        source(
          """
            data class Holder(val keyUi: KeyUi<*>)
            val keyUi = KeyUi<Any> {}
          """
        )
      ),
      listOf(
        invokableSource(
          """
            fun invoke(): @Composable () -> Unit = {
              Holder(keyUi)
            }
          """
        )
      )
    ),
    config = { withCompose() }
  ) {
    runComposing {
      invokeSingleFile<@Composable () -> Unit>().invoke()
    }
  }

  @Test fun testComposeFunInterfaceWithFunctionSuperType3() = multiCodegen(
    listOf(
      listOf(
        source(
          """
            fun interface KeyUi<K> : @Composable () -> Unit
          """
        )
      ),
      listOf(
        source(
          """
            val keyUi: KeyUi<Any>? = KeyUi<Any> {}
          """
        )
      ),
      listOf(
        invokableSource(
          """
            fun invoke(): @Composable () -> Unit = {
              keyUi?.invoke()
            }
          """
        )
      )
    ),
    config = { withCompose() }
  ) {
    runComposing {
      invokeSingleFile<@Composable () -> Unit>().invoke()
    }
  }

  @Test fun testComposableFunInterfaceWithComposableFunction() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            fun interface ModelKeyUi<K, M> {
              @Composable operator fun invoke(scope: ModelKeyUiScope<K, M>)
            }
      
            interface ModelKeyUiScope<K, M> {
              val model: M
            }
          """
        )
      ),
      listOf(
        source(
          """
            val testKeyUi = ModelKeyUi<String, Int> {
              val test = remember { it.model }
            }
          """
        )
      ),
      listOf(
        invokableSource(
          """
            fun invoke(): @Composable () -> Unit = {
              testKeyUi.invoke(
                object : ModelKeyUiScope<String, Int> {
                  override val model: Int get() = 0
                }
              )
            }
          """
        )
      )
    ),
    config = { withCompose() }
  ) {
    runComposing {
      invokeSingleFile<@Composable () -> Unit>().invoke()
    }
  }
}
