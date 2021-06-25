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

package com.ivianuu.injekt.compose

import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.*
import androidx.test.ext.junit.runners.*
import com.ivianuu.injekt.*
import com.ivianuu.injekt.ambient.*
import io.kotest.matchers.*
import org.junit.*
import org.junit.runner.*

@RunWith(AndroidJUnit4::class)
class LocalAmbientsTest {
  @get:Rule val composeRule = createComposeRule()

  @Test fun testLocalAmbients() {
    @Provide val ambientInt = ambientOf { 0 }
    composeRule.setContent {
      CompositionLocalProvider(LocalAmbients provides ambientsOf(provide(42))) {
        current<Int>() shouldBe 42
      }
    }
  }
}
