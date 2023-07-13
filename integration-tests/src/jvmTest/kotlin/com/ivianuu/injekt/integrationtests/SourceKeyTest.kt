/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import io.kotest.matchers.shouldBe
import org.junit.Test

class SourceKeyTest {
  @Test fun testSourceKey() = codegen(
    """
      fun invoke() = sourceKey()
    """
  ) {
    invokeSingleFile() shouldBe "File.kt:11:21"
  }

  @Test fun testSourceKeyPassing() = codegen(
    """
      context(SourceKey) fun a() = this@SourceKey
      context(SourceKey) fun b() = a()
      fun invoke() = b()
    """
  ) {
    invokeSingleFile() shouldBe "File.kt:13:21"
  }
}
