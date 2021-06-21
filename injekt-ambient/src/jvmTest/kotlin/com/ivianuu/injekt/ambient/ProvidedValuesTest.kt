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

package com.ivianuu.injekt.ambient

import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.*
import io.kotest.matchers.*
import org.junit.*

class ProvidedValuesTest {
  @Test fun testNamedProvidedValue() {
    val ambient = ambientOf { 0 }
    @Provide val providedInt: NamedProvidedValue<ForApp, Int> = ambient provides 42
    @Provide val ambients = ambientsOf<ForApp>()
    ambient.current() shouldBe 42
  }

  @Test fun testProvidedValuesFactoryModule() {
    val ambient = ambientOf { 0 }
    @Provide val childProvidedValuesModule = ProvidedValuesFactoryModule0<ForApp, ForChild>()
    @Provide val providedInt: NamedProvidedValue<ForApp, Int> = ambient provides 42
    @Provide val parentAmbients = ambientsOf<ForApp>()
    withInstances(createAmbientsFromProvidedValues<ForChild>()) {
      ambient.current() shouldBe 42
    }
  }

  private abstract class ForChild private constructor()
}
