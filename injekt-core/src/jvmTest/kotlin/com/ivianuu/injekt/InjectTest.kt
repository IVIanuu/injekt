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

package com.ivianuu.injekt

import io.kotest.matchers.*
import io.kotest.matchers.nulls.*
import org.junit.*

class InjectTest {
  @Test fun testInject() {
    @Provide val value = "42"
    inject<String>() shouldBe "42"
  }

  @Test fun testInjectOrNullReturnsProvidedInstance() {
    @Provide val value = "42"
    injectOrNull<String>() shouldBe "42"
  }

  @Test fun testInjectOrNullReturnsNullForNotProvidedInstance() {
    injectOrNull<String>().shouldBeNull()
  }
}
