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

import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.shouldContainMessage
import com.ivianuu.injekt.test.shouldNotContainMessage
import com.ivianuu.injekt.test.singleAndMultiCodegen
import com.ivianuu.injekt.test.source
import org.jetbrains.kotlin.name.FqName
import org.junit.Test

class ImportSuggestionsTest {
  @Test fun testShowsUsefulImportSuggestion() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            object FooModule {
              @Provide fun foo() = Foo()
            }
          """,
          packageFqName = FqName("modules")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = inject<Foo>()
          """
        )
      )
    )
  ) {
    compilationShouldHaveFailed()
    shouldContainMessage("@Providers(\"modules.FooModule.foo\")")
  }

  @Test fun testDoesNotShowUnrelatedSuggestion() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            object BarModule {
              @Provide fun bar(): Bar = TODO()
            }
          """,
          packageFqName = FqName("modules")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = inject<Foo>()
          """
        )
      )
    )
  ) {
    compilationShouldHaveFailed()
    shouldNotContainMessage("might fix the problem")
  }
}
