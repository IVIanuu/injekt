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
import io.kotest.matchers.*
import org.junit.*

class AmbientsTest {
  @Test fun testGetReturnsProvidedValue() {
    @Provide val ambient = ambientOf { 0 }
    @Provide val ambients = ambientsOf(provide(42))
    current<Int>() shouldBe 42
  }

  @Test fun testGetReturnsDefaultIfNoValueIsProvided() {
    @Provide val ambient = ambientOf { 42 }
    @Provide val ambients = ambientsOf()
    current<Int>() shouldBe 42
  }

  @Test fun testProvidesDoesOverrideExistingValue() {
    @Provide val ambient = ambientOf { 0 }
    val baseAmbients = ambientsOf(provide(1))
    current<Int>(baseAmbients) shouldBe 1
    val finalAmbients = ambientsOf(provide(42))
    current<Int>(finalAmbients) shouldBe 42
  }

  @Test fun testProvidesDefaultDoesNotOverrideExistingValue() {
    @Provide val ambient = ambientOf { 0 }
    val baseAmbients = ambientsOf(provide(42))
    val finalAmbients = baseAmbients.plus(provideDefault(1))
    current<Int>(finalAmbients) shouldBe 42
  }

  @Test fun testPlus() {
    @Provide val ambient = ambientOf { 0 }
    val baseAmbients = ambientsOf()
    current<Int>(baseAmbients) shouldBe 0
    val finalAmbients = baseAmbients + provide(42)
    current<Int>(finalAmbients) shouldBe 42
  }

  @Test fun testMinus() {
    @Provide val ambient = ambientOf { 42 }
    val baseAmbients = ambientsOf(provide(0))
    current<Int>(baseAmbients) shouldBe 0
    val finalAmbients = baseAmbients.minus<Int>()
    current<Int>(finalAmbients) shouldBe 42
  }
}
