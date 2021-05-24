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
    compilationShouldHaveFailed("unresolved injectable import")
  }

  @Test fun testUnresolvedStarImport() = codegen(
    """
            @Providers("a.*")
      fun invoke() {
            }
    """
  ) {
    compilationShouldHaveFailed("unresolved injectable import")
  }

  @Test fun testImportsJustAPackage() = codegen(
    """
      @Providers("kotlin.collections")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("unresolved injectable import")
  }

  @Test fun testMalformedImport() = codegen(
    """
      @Providers("-_;-")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("cannot read injectable import")
  }

  @Test fun testDuplicatedImports() = codegen(
    """
      @Providers("kotlin.collections.*", "kotlin.collections.*")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("duplicated injectable import")
  }

  @Test fun testNestedDuplicatedImports() = codegen(
    """
      @Providers("kotlin.collections.*")
      fun invoke() {
        withProviders("kotlin.collections.*") {
        }
      }
    """
  ) {
    compilationShouldHaveFailed("duplicated injectable import")
  }

  @Test fun testUnusedImport() = codegen(
    """
      @Providers("kotlin.collections.*")
      fun invoke() {
      }
    """
  ) {
    shouldContainMessage("unused injectable import")
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
    shouldNotContainMessage("unused injectable import")
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
    shouldNotContainMessage("unused injectable import")
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

  @Test fun testImportInjectableSamePackage() = codegen(
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

  @Test fun testUseClassImportsInConstructor() = singleAndMultiCodegen(
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
              val foo: Foo
              constructor() {
                foo = inject()
              }
            }

            fun invoke() = MyClass().foo
          """,
          name = "File.kt"
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testUseClassImportsInInitializer() = singleAndMultiCodegen(
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
              val foo: Foo = inject()
            }
            fun invoke() = MyClass().foo
          """,
          name = "File.kt"
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testClassImportsInSuperTypeDelegateExpression() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val foo = Foo()
            interface FooHolder {
              val foo: Foo
            }
            fun FooHolder(@Inject foo: Foo) = object : FooHolder {
              override val foo: Foo = foo
            }
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        source(
          """
            @Providers("injectables.*")
            class MyClass : injectables.FooHolder by injectables.FooHolder()
            fun invoke() = MyClass().foo
          """,
          name = "File.kt"
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testPrimaryConstructorImportsInSuperTypeDelegateExpression() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val foo = Foo()
            interface FooHolder {
              val foo: Foo
            }
            fun FooHolder(@Inject foo: Foo) = object : FooHolder {
              override val foo: Foo = foo
            }
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        source(
          """
            class MyClass @Providers("injectables.*") constructor() : injectables.FooHolder by injectables.FooHolder()
            fun invoke() = MyClass().foo
          """,
          name = "File.kt"
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testFunctionWithInjectableImports() = singleAndMultiCodegen(
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

  @Test fun testFunctionImportsInDefaultValueExpression() = singleAndMultiCodegen(
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
            @JvmOverloads
            fun invoke(foo: Foo = inject()) = foo
          """,
          name = "File.kt"
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testPropertyWithInjectableImports() = singleAndMultiCodegen(
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
            val injectableFoo = inject<Foo>()
            fun invoke() = injectableFoo
          """,
          name = "File.kt"
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testWithInjectableImports() = codegen(
    listOf(
      source(
        """
          fun invoke() = withProviders("com.ivianuu.injekt.common.*") {
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
