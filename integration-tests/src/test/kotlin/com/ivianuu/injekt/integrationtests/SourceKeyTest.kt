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
    invokeSingleFile() shouldBe "File.kt:14:21"
  }

  @Test fun testSourceKeyPassing() = codegen(
    """
      fun a(@Inject key: SourceKey) = key
      fun b(@Inject key: SourceKey) = a()
      fun invoke() = b()
    """
  ) {
    invokeSingleFile() shouldBe "File.kt:16:21"
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
        SourceKey("File.kt:16:21"),
        SourceKey("File.kt:15:44")
      )
  }

  @Test fun testSpreadingInjectableWithSourceKeyTarget() = codegen(
    """
      @Provide fun <@Spread T : SourceKey> taggedSourceKey(t: T): @Tag1 SourceKey = t

      fun invoke() = inject<@Tag1 SourceKey>()
    """
  )
}
