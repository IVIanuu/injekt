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

package com.ivianuu.injekt.common

import com.ivianuu.injekt.*
import io.kotest.matchers.*
import io.kotest.matchers.nulls.*
import org.junit.*
import kotlin.reflect.*

class CommonInjectablesTest {
  @Test fun testCanUseMapForSetOfPairs() {
    @Provide val elementsA = setOf("a" to "a")
    @Provide val elementB = setOf("b" to "b")
    val map = injectOrNull<Map<String, String>>()
    map.shouldNotBeNull()
    map.size shouldBe 2
    map["a"] shouldBe "a"
    map["b"] shouldBe "b"
  }

  @Test fun testCanUseLazy() {
    injectOrNull<Lazy<Foo>>().shouldNotBeNull()
  }

  @Test fun testCanUseKClass() {
    injectOrNull<KClass<Foo>>().shouldNotBeNull()
  }

  @Test fun testCanUseType() {
    injectOrNull<TypeKey<Foo>>().shouldNotBeNull()
  }

  @Provide private class Foo
}
