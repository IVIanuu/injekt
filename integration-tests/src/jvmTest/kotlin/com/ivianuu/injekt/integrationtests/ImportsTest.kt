/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Bar
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokableSource
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.shouldContainMessage
import com.ivianuu.injekt.test.shouldNotContainMessage
import com.ivianuu.injekt.test.singleAndMultiCodegen
import com.ivianuu.injekt.test.source
import io.kotest.matchers.types.shouldBeTypeOf
import org.jetbrains.kotlin.name.FqName
import org.junit.Test

class ImportsTest {
  @Test fun testUnresolvedImport() = codegen(
    """
      @Providers("a")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("unresolved provider import")
  }

  @Test fun testUnresolvedStarImport() = codegen(
    """
      @Providers("a.*")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("unresolved provider import")
  }

  @Test fun testUnresolvedDoubleStarImport() = codegen(
    """
      @Providers("a.**")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("unresolved provider import")
  }

  @Test fun testImportsJustAPackage() = codegen(
    """
      @Providers("kotlin.collections")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("unresolved provider import")
  }

  @Test fun testMalformedImport() = codegen(
    """
      @Providers("-_;-")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("cannot read provider import")
  }

  @Test fun testDuplicatedImports() = codegen(
    """
      @Providers("kotlin.collections.*", "kotlin.collections.*")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("duplicated provider import")
  }

  @Test fun testUnusedImport() = codegen(
    """
      @Providers("kotlin.collections.*")
      fun invoke() {
      }
    """
  ) {
    shouldContainMessage("unused provider import")
  }

  @Test fun testUsedProviderImport() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val foo = Foo()
          """,
          packageFqName = FqName("providers")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("providers.foo")
            fun invoke() = context<Foo>()
          """
        )
      )
    )
  ) {
    shouldNotContainMessage("unused provider import")
  }

  @Test fun testClassImportIsNotMarkedUnusedIfACompanionClassProviderWasUsed() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            class MyClass {
              companion object {
                @Provide class FooProvider {
                  @Provide val foo = Foo()
                }
              }
            }
          """,
          packageFqName = FqName("providers")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("providers.MyClass")
            fun invoke() = context<Foo>()
          """
        )
      )
    )
  ) {
    shouldNotContainMessage("unused provider import")
  }

  @Test fun testUsedStarImport() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val foo = Foo()
          """,
          packageFqName = FqName("providers")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("providers.*")
            fun invoke() = context<Foo>()
          """
        )
      )
    )
  ) {
    shouldNotContainMessage("unused provider import")
  }

  @Test fun testUsedDoubleStarImport() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val foo = Foo()
          """,
          packageFqName = FqName("providers")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("providers.**")
            fun invoke() = context<Foo>()
          """
        )
      )
    )
  ) {
    shouldNotContainMessage("unused provider import")
  }

  @Test fun testExplicitAndStarImportMarksStarAsUnused() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val value = "explicit"
          """,
          packageFqName = FqName("explicit")
        ),
        source(
          """
            @Provide val value = "star"
          """,
          packageFqName = FqName("star")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("explicit.value", "star.*")
            fun invoke() = context<String>()
        """
        )
      )
    )
  ) {
    shouldContainMessage("unused provider import: 'star.*'")
  }

  @Test fun testExplicitAndDoubleStarImportMarksDoubleStarAsUnused() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val value = "explicit"
          """,
          packageFqName = FqName("explicit")
        ),
        source(
          """
            @Provide val value = "star"
          """,
          packageFqName = FqName("star")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("explicit.value", "star.**")
            fun invoke() = context<String>()
        """
        )
      )
    )
  ) {
    shouldContainMessage("unused provider import: 'star.**'")
  }

  @Test fun testStarImportSamePackage() = codegen(
    """
      @Providers("com.ivianuu.injekt.integrationtests.*")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("providers of the same package are automatically imported")
  }

  @Test fun testDoubleStarImportSamePackage() = codegen(
    """
      @Providers("com.ivianuu.injekt.integrationtests.**")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("providers of the same package are automatically imported")
  }

  @Test fun testImportProvidersamePackage() = codegen(
    """
      @Provide val foo = Foo()
      @Providers("com.ivianuu.injekt.integrationtests.foo")
      fun invoke() {
      }
    """
  ) {
    compilationShouldHaveFailed("providers of the same package are automatically imported")
  }

  @Test fun testClassWithImports() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val foo = Foo()
          """,
          packageFqName = FqName("providers")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("providers.*")
            class MyClass {
              fun invoke() = context<Foo>()
            }
            fun invoke() = MyClass().invoke()
          """
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
          packageFqName = FqName("providers")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("providers.*")
            class MyClass {
              val foo: Foo
              constructor() {
                foo = context()
              }
            }

            fun invoke() = MyClass().foo
          """
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
          packageFqName = FqName("providers")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("providers.*")
            class MyClass {
              val foo: Foo = context()
            }
            fun invoke() = MyClass().foo
          """
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
            fun FooHolder(@Context foo: Foo) = object : FooHolder {
              override val foo: Foo = foo
            }
          """,
          packageFqName = FqName("providers")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("providers.*")
            class MyClass : providers.FooHolder by providers.FooHolder()
            fun invoke() = MyClass().foo
          """
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testObjectImportsInSuperTypeDelegateExpression() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val foo = Foo()
            interface FooHolder {
              val foo: Foo
            }
            fun FooHolder(@Context foo: Foo) = object : FooHolder {
              override val foo: Foo = foo
            }
          """,
          packageFqName = FqName("providers")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("providers.*")
            object MyObject : providers.FooHolder by providers.FooHolder()
            fun invoke() = MyObject.foo
          """
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
            fun FooHolder(@Context foo: Foo) = object : FooHolder {
              override val foo: Foo = foo
            }
          """,
          packageFqName = FqName("providers")
        )
      ),
      listOf(
        invokableSource(
          """
            class MyClass @Providers("providers.*") constructor() : providers.FooHolder by providers.FooHolder()
            fun invoke() = MyClass().foo
          """
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testFunctionWithProviderImports() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val foo = Foo()
          """,
          packageFqName = FqName("providers")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("providers.*")
            fun invoke() = context<Foo>()
          """
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
          packageFqName = FqName("providers")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("providers.*")
            @JvmOverloads
            fun invoke(foo: Foo = context()) = foo
          """
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testPropertyWithProviderImports() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val foo = Foo()
          """,
          packageFqName = FqName("providers")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("providers.*")
            val providerFoo = context<Foo>()
            fun invoke() = providerFoo
          """
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testLocalVariableWithProviderImports() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val foo = Foo()
          """,
          packageFqName = FqName("providers")
        )
      ),
      listOf(
        invokableSource(
          """
            fun invoke(): Foo {
              @Providers("providers.*")
              val providerFoo = context<Foo>()
              return providerFoo
            }
          """
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testExpressionWithProviderImports() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val foo = Foo()
          """,
          packageFqName = FqName("providers")
        )
      ),
      listOf(
        invokableSource(
          """
            fun invoke(): Foo {
              @Providers("providers.*")
              return context<Foo>()
            }
          """
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testSingleStarImportNotImportsSubObjects() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            object FooModule {
              @Provide val foo = Foo()
              
              object BarModule {
                @Provide fun bar(foo: Foo) = Bar(foo)
              }
            }
          """,
          packageFqName = FqName("providers")
        )
      ),
      listOf(
        invokableSource(
          """
            fun invoke(): Bar {
              @Providers("providers.*")
              return context<Bar>()
            }
          """
        )
      )
    )
  ) {
    compilationShouldHaveFailed("no provider found of type com.ivianuu.injekt.test.Bar for parameter x of function com.ivianuu.injekt.context.")
  }

  @Test fun testDoubleStarImportImportsSubPackages() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val foo = Foo()
          """,
          packageFqName = FqName("providers.foo")
        ),
        source(
          """
            @Provide fun bar(foo: Foo) = Bar(foo)
          """,
          packageFqName = FqName("providers.foo.bar")
        )
      ),
      listOf(
        invokableSource(
          """
            fun invoke(): Bar {
              @Providers(".**")
              return context<Bar>()
            }
          """
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Bar>()
  }

  @Test fun testDoubleStarImportImportsSubObjects() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            object FooModule {
              @Provide val foo = Foo()
              object BarModule {
                @Provide fun bar(foo: Foo) = Bar(foo)
              }
            }
          """,
          packageFqName = FqName("providers")
        )
      ),
      listOf(
        invokableSource(
          """
            fun invoke(): Bar {
              @Providers(".**")
              return context<Bar>()
            }
          """
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Bar>()
  }
}
