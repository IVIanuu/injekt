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

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.inject
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

class WithFallbackTest {
  @Test fun prefersPreferred() {
    @Provide val preferred = ""
    @Provide val fallback = 0

    val value = inject<WithFallback<String, Int>>()

    value.shouldBeInstanceOf<WithFallback.Preferred<String>>()
    value.value shouldBe preferred
  }

  @Test fun usesFallback() {
    @Provide val fallback = 0

    val value = inject<WithFallback<String, Int>>()

    value.shouldBeInstanceOf<WithFallback.Fallback<Int>>()
    value.value shouldBe 0
  }
}
