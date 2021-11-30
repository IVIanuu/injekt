/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.common.SourceKey
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test

class SourceKeyTest {
  @Test fun testSourceKey() = codegen(
    """
      fun invoke() = sourceKey()
    """
  ) {
    invokeSingleFile() shouldBe "File.kt:12:21"
  }

  @Test fun testSourceKeyPassing() = codegen(
    """
      fun a(@Inject key: SourceKey) = key
      fun b(@Inject key: SourceKey) = a()
      fun invoke() = b()
    """
  ) {
    invokeSingleFile() shouldBe "File.kt:14:21"
  }

  @Test fun testListOfSourceKeys() = codegen(
    """
      fun a(@Inject keys: List<SourceKey>) = keys
      fun b(@Inject key: List<SourceKey>) = a()
      fun invoke() = b()
    """
  ) {
    invokeSingleFile<List<SourceKey>>()
      .shouldContainExactly(
        SourceKey("File.kt:14:21"),
        SourceKey("File.kt:13:44")
      )
  }
}
