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
import io.kotest.matchers.types.*
import org.jetbrains.kotlin.name.*
import org.junit.*

class GivenImportsTest {
  @Test fun testUnresolvedImport() = codegen(
    """
      @ProvideImports("a")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("Unresolved given import")
  }

  @Test fun testUnresolvedStarImport() = codegen(
    """
            @ProvideImports("a.*")
      fun invoke() {
            }
    """
  ) {
    compilationShouldHaveFailed("Unresolved given import")
  }

  @Test fun testImportsJustAPackage() = codegen(
    """
      @ProvideImports("kotlin.collections")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("Unresolved given import")
  }

  @Test fun testMalformedImport() = codegen(
    """
      @ProvideImports("-_;-")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("Cannot read given import")
  }

  @Test fun testDuplicatedImports() = codegen(
    """
      @ProvideImports("kotlin.collections.*", "kotlin.collections.*")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("Duplicated given import")
  }

  @Test fun testNestedDuplicatedImports() = codegen(
    """
      @ProvideImports("kotlin.collections.*")
      fun invoke() {
        withGivenImports("kotlin.collections.*") {
        }
      }
    """
  ) {
    compilationShouldHaveFailed("Duplicated given import")
  }

  @Test fun testUnusedImport() = codegen(
    """
      @ProvideImports("kotlin.collections.*")
      fun invoke() {
      }
    """
  ) {
    shouldContainMessage("Unused given import")
  }

  @Test fun testUsedImport() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val foo = Foo()
          """,
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            @ProvideImports("givens.foo")
            fun invoke() = inject<Foo>()
          """,
          name = "File.kt"
        )
      )
    )
  ) {
    shouldNotContainMessage("Unused given import")
  }

  @Test fun testUsedStarImport() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val foo = Foo()
          """,
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            @ProvideImports("givens.*")
            fun invoke() = inject<Foo>()
          """,
          name = "File.kt"
        )
      )
    )
  ) {
    shouldNotContainMessage("Unused given import")
  }

  @Test fun testStarImportSamePackage() = codegen(
    """
      @ProvideImports("com.ivianuu.injekt.integrationtests.*")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("Givens of the same package are automatically imported")
  }

  @Test fun testImportGivenSamePackage() = codegen(
    """
      @Provide val foo = Foo()
      @ProvideImports("com.ivianuu.injekt.integrationtests.foo")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("Givens of the same package are automatically imported")
  }

  @Test fun testClassWithGivenImports() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val foo = Foo()
          """,
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            @ProvideImports("givens.*")
            class MyClass {
              fun invoke() = inject<Foo>()
            }
            fun invoke() = MyClass().invoke()
          """,
          name = "File.kt"
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testFunctionWithGivenImports() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val foo = Foo()
          """,
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            @ProvideImports("givens.*")
            fun invoke() = inject<Foo>()
          """,
          name = "File.kt"
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testPropertyWithGivenImports() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val foo = Foo()
          """,
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            @ProvideImports("givens.*")
            val givenFoo = inject<Foo>()
            fun invoke() = givenFoo
          """,
          name = "File.kt"
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testWithGivenImports() = codegen(
    listOf(
      source(
        """
          fun invoke() = withGivenImports("com.ivianuu.injekt.common.*") {
            inject<TypeKey<Foo>>().value
          }
        """,
        name = "File.kt"
      )
    )
  ) {
    invokeSingleFile() shouldBe "com.ivianuu.injekt.test.Foo"
  }
}
