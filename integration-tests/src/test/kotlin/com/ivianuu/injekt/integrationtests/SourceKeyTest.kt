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

import com.ivianuu.injekt.test.*
import io.kotest.matchers.*
import org.junit.*

class SourceKeyTest {
  @Test fun testSourceKeyInFunction() = codegen(
    """
      fun invoke() = sourceKey()
    """
  ) {
    invokeSingleFile() shouldBe "File.kt:com.ivianuu.injekt.integrationtests.invoke:17:21"
  }

  @Test fun testSourceKeyInProperty() = codegen(
    """
      val key = sourceKey()
      fun invoke() = key
    """
  ) {
    invokeSingleFile() shouldBe "File.kt:com.ivianuu.injekt.integrationtests.key:17:16"
  }

  @Test fun testSourceKeyInPropertyGetter() = codegen(
    """
      val key get() = sourceKey()
      fun invoke() = key
    """
  ) {
    invokeSingleFile() shouldBe "File.kt:com.ivianuu.injekt.integrationtests.key:17:22"
  }

  @Test fun testSourceKeyInLambda() = codegen(
    """
      fun invoke() = { { sourceKey() }() }()
    """
  ) {
    invokeSingleFile() shouldBe "File.kt:com.ivianuu.injekt.integrationtests.invoke:17:25"
  }

  @Test fun testSourceKeyInClassInitializer() = codegen(
    """
      class MyClass {
        val key: String
        init {
          key = sourceKey().value
        }
      }
      fun invoke() = MyClass().key
    """
  ) {
    invokeSingleFile() shouldBe "File.kt:com.ivianuu.injekt.integrationtests.MyClass:20:16"
  }

  @Test fun testSourceKeyInLambdaInClassInitializer() = codegen(
    """
      class MyClass {
        val key: String
        init {
          key = { sourceKey().value }()
        }
      }
      fun invoke() = MyClass().key
    """
  ) {
    invokeSingleFile() shouldBe "File.kt:com.ivianuu.injekt.integrationtests.MyClass:20:18"
  }

  @Test fun testSourceKeyPassing() = codegen(
    """
      fun a(@Inject key: SourceKey) = key
      fun b(@Inject key: SourceKey) = a()
      fun invoke() = b()
    """
  ) {
    invokeSingleFile() shouldBe "File.kt:com.ivianuu.injekt.integrationtests.invoke:19:21"
  }
}
