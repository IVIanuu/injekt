/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.*
import io.kotest.matchers.types.*
import org.jetbrains.kotlin.name.*
import org.junit.*

class ImportsTest {
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

  @Test fun testUnresolvedDoubleStarImport() = codegen(
    """
      @Providers("a.**")
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

  @Test fun testUnusedImport() = codegen(
    """
      @Providers("kotlin.collections.*")
      fun invoke() {
      }
    """
  ) {
    shouldContainMessage("unused injectable import")
  }

  @Test fun testUsedInjectableImport() = singleAndMultiCodegen(
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
        invokableSource(
          """
            @Providers("injectables.foo")
            fun invoke() = inject<Foo>()
          """
        )
      )
    )
  ) {
    shouldNotContainMessage("unused injectable import")
  }

  @Test fun testClassImportIsNotMarkedUnusedIfACompanionClassInjectableWasUsed() = singleAndMultiCodegen(
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
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("injectables.MyClass")
            fun invoke() = inject<Foo>()
          """
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
        invokableSource(
          """
            @Providers("injectables.*")
            fun invoke() = inject<Foo>()
          """
        )
      )
    )
  ) {
    shouldNotContainMessage("unused injectable import")
  }

  @Test fun testUsedDoubleStarImport() = singleAndMultiCodegen(
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
        invokableSource(
          """
            @Providers("injectables.**")
            fun invoke() = inject<Foo>()
          """
        )
      )
    )
  ) {
    shouldNotContainMessage("unused injectable import")
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
            fun invoke() = inject<String>()
        """
        )
      )
    )
  ) {
    shouldContainMessage("unused injectable import: 'star.*'")
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
            fun invoke() = inject<String>()
        """
        )
      )
    )
  ) {
    shouldContainMessage("unused injectable import: 'star.**'")
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

  @Test fun testDoubleStarImportSamePackage() = codegen(
    """
      @Providers("com.ivianuu.injekt.integrationtests.**")
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
        invokableSource(
          """
            @Providers("injectables.*")
            class MyClass {
              fun invoke() = inject<Foo>()
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
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("injectables.*")
            class MyClass {
              val foo: Foo
              constructor() {
                foo = inject()
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
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("injectables.*")
            class MyClass {
              val foo: Foo = inject()
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
            fun FooHolder(foo: Foo) = object : FooHolder {
              override val foo: Foo = foo
            }
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("injectables.*")
            class MyClass : injectables.FooHolder by injectables.FooHolder(inject())
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
            fun FooHolder(foo: Foo) = object : FooHolder {
              override val foo: Foo = foo
            }
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("injectables.*")
            object MyObject : injectables.FooHolder by injectables.FooHolder(inject())
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
            fun FooHolder(foo: Foo) = object : FooHolder {
              override val foo: Foo = foo
            }
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        invokableSource(
          """
            class MyClass @Providers("injectables.*") constructor() : injectables.FooHolder by injectables.FooHolder(inject())
            fun invoke() = MyClass().foo
          """
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
        invokableSource(
          """
            @Providers("injectables.*")
            fun invoke() = inject<Foo>()
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
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("injectables.*")
            @JvmOverloads
            fun invoke(foo: Foo = inject()) = foo
          """
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
        invokableSource(
          """
            @Providers("injectables.*")
            val injectableFoo = inject<Foo>()
            fun invoke() = injectableFoo
          """
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testLocalVariableWithInjectableImports() = singleAndMultiCodegen(
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
        invokableSource(
          """
            fun invoke(): Foo {
              @Providers("injectables.*")
              val injectableFoo = inject<Foo>()
              return injectableFoo
            }
          """
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testExpressionWithInjectableImports() = singleAndMultiCodegen(
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
        invokableSource(
          """
            fun invoke(): Foo {
              @Providers("injectables.*")
              return inject<Foo>()
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
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        invokableSource(
          """
            fun invoke(): Bar {
              @Providers("injectables.*")
              return inject<Bar>()
            }
          """
        )
      )
    )
  ) {
    compilationShouldHaveFailed("no injectable found of type com.ivianuu.injekt.test.Bar for parameter x of function com.ivianuu.injekt.inject.")
  }

  @Test fun testDoubleStarImportImportsSubPackages() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide val foo = Foo()
          """,
          packageFqName = FqName("injectables.foo")
        ),
        source(
          """
            @Provide fun bar(foo: Foo) = Bar(foo)
          """,
          packageFqName = FqName("injectables.foo.bar")
        )
      ),
      listOf(
        invokableSource(
          """
            fun invoke(): Bar {
              @Providers(".**")
              return inject<Bar>()
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
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        invokableSource(
          """
            fun invoke(): Bar {
              @Providers(".**")
              return inject<Bar>()
            }
          """
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Bar>()
  }

  @Test fun testImportedProvideFunctionInObject() = singleAndMultiCodegen(
    """
      object FooModule {
        @Provide fun foo() = Foo()
      }
    """,
    """
      @Providers("com.ivianuu.injekt.integrationtests.FooModule.foo")
      fun invoke() = inject<Foo>() 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testImportedProvideFunctionInObjectWithStar() = singleAndMultiCodegen(
    """
      object FooModule {
        @Provide fun foo() = Foo()
      }
    """,
    """
      @Providers("com.ivianuu.injekt.integrationtests.FooModule.*")
      fun invoke() = inject<Foo>() 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testImportedProvideFunctionInCompanionObject() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            class FooModule {
              companion object {
                @Provide fun foo() = Foo()
              }
            }
          """,
          packageFqName = FqName("providers")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("providers.FooModule")
            fun invoke() = inject<Foo>()
          """
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }
}
