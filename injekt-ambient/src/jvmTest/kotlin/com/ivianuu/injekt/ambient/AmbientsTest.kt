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

import io.kotest.matchers.*
import org.junit.*

class AmbientsTest {
  @Test fun testGetReturnsProvidedValue() {
    val ambient = ambientOf { 0 }
    val ambients = ambientsOf(ambient provides 42)
    ambient.current(ambients) shouldBe 42
  }

  @Test fun testGetReturnsDefaultIfNoValueIsProvided() {
    val ambient = ambientOf { 42 }
    val ambients = ambientsOf()
    ambient.current(ambients) shouldBe 42
  }

  @Test fun testProvidesDoesOverrideExistingValue() {
    val ambient = ambientOf { 0 }
    val baseAmbients = ambientsOf(ambient provides 1)
    ambient.current(baseAmbients) shouldBe 1
    val finalAmbients = ambientsOf(ambient provides 42)
    ambient.current(finalAmbients) shouldBe 42
  }

  @Test fun testProvidesDefaultDoesNotOverrideExistingValue() {
    val ambient = ambientOf { 0 }
    val baseAmbients = ambientsOf(ambient provides 42)
    val finalAmbients = baseAmbients.plus(ambient providesDefault 1)
    ambient.current(finalAmbients) shouldBe 42
  }

  @Test fun testPlus() {
    val ambient = ambientOf { 0 }
    val baseAmbients = ambientsOf()
    ambient.current(baseAmbients) shouldBe 0
    val finalAmbients = baseAmbients.plus(ambient provides 42)
    ambient.current(finalAmbients) shouldBe 42
  }

  @Test fun testMinus() {
    val ambient = ambientOf { 42 }
    val baseAmbients = ambientsOf(ambient provides 0)
    ambient.current(baseAmbients) shouldBe 0
    val finalAmbients = baseAmbients.minus(ambient)
    ambient.current(finalAmbients) shouldBe 42
  }
}
