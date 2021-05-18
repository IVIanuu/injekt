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

class InjectablesImportsTest {
  @Test fun testUnresolvedImport() = codegen(
    """
      @Providers("a")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("Unresolved injectable import")
  }

  @Test fun testUnresolvedStarImport() = codegen(
    """
            @Providers("a.*")
      fun invoke() {
            }
    """
  ) {
    compilationShouldHaveFailed("Unresolved injectable import")
  }

  @Test fun testImportsJustAPackage() = codegen(
    """
      @Providers("kotlin.collections")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("Unresolved injectable import")
  }

  @Test fun testMalformedImport() = codegen(
    """
      @Providers("-_;-")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("Cannot read injectable import")
  }

  @Test fun testDuplicatedImports() = codegen(
    """
      @Providers("kotlin.collections.*", "kotlin.collections.*")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("Duplicated injectable import")
  }

  @Test fun testNestedDuplicatedImports() = codegen(
    """
      @Providers("kotlin.collections.*")
      fun invoke() {
        withGivenImports("kotlin.collections.*") {
        }
      }
    """
  ) {
    compilationShouldHaveFailed("Duplicated injectable import")
  }

  @Test fun testUnusedImport() = codegen(
    """
      @Providers("kotlin.collections.*")
      fun invoke() {
      }
    """
  ) {
    shouldContainMessage("Unused injectable import")
  }

  @Test fun testUsedImport() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val foo = Foo()
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        source(
          """
            @Providers("injectables.foo")
            fun invoke() = inject<Foo>()
          """,
          name = "File.kt"
        )
      )
    )
  ) {
    shouldNotContainMessage("Unused injectable import")
  }

  @Test fun testUsedStarImport() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val foo = Foo()
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        source(
          """
            @Providers("injectables.*")
            fun invoke() = inject<Foo>()
          """,
          name = "File.kt"
        )
      )
    )
  ) {
    shouldNotContainMessage("Unused injectable import")
  }

  @Test fun testStarImportSamePackage() = codegen(
    """
      @Providers("com.ivianuu.injekt.integrationtests.*")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("injectables of the same package are automatically imported")
  }

  @Test fun testImportGivenSamePackage() = codegen(
    """
      @Provide val foo = Foo()
      @Providers("com.ivianuu.injekt.integrationtests.foo")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("injectables of the same package are automatically imported")
  }

  @Test fun testClassWithImports() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val foo = Foo()
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        source(
          """
            @Providers("injectables.*")
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
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        source(
          """
            @Providers("injectables.*")
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
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        source(
          """
            @Providers("injectables.*")
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
