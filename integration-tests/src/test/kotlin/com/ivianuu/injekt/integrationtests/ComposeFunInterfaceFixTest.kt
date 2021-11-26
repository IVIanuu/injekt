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

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.singleAndMultiCodegen
import com.ivianuu.injekt.test.withCompose
import org.junit.Test

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

  @Test fun testComposableFunInterfaceWithComposableExtensionFunction() = multiCodegen(
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
