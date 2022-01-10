/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.*
import org.jetbrains.kotlin.name.*
import org.junit.*

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

  @Test fun testShowsUsefulImportSuggestionForNestedError() = singleAndMultiCodegen(
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
            @Provide fun bar(foo: Foo) = Bar(foo)
            fun invoke() = inject<Bar>()
          """
        )
      )
    )
  ) {
    compilationShouldHaveFailed()
    shouldContainMessage("@Providers(\"modules.FooModule.foo\")")
  }
}
