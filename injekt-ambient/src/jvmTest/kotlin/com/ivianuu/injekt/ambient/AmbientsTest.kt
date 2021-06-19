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
import io.kotest.matchers.nulls.*
import org.junit.*

class AmbientsTest {
  @Test fun testGetReturnsProvidedValue() {
    val ambient = ambientOf { 0 }
    val ambients = ambientsOf(ambient provides 42)
    ambients[ambient] shouldBe 42
  }

  @Test fun testGetReturnsDefaultIfNoValueIsProvided() {
    val ambient = ambientOf { 42 }
    val ambients = ambientsOf()
    ambients[ambient] shouldBe 42
  }

  @Test fun testBaseAmbients() {
    @Provide val base = ambientsOf()

    AmbientBaseAmbients.current().shouldBeNull()

    withInstances(base.plus(AmbientDummy provides Unit)) {
      AmbientBaseAmbients.current() shouldBe base
    }
  }

  private val AmbientDummy = ambientOf<Unit> { Unit }
}
