/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.*
import io.kotest.matchers.types.*
import org.junit.*

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
