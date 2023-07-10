/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.invokableSource
import com.ivianuu.injekt.test.singleAndMultiCodegen
import com.ivianuu.injekt.test.source
import org.jetbrains.kotlin.name.FqName
import org.junit.Test

class TypeScopeTest {
  @Test fun testClassTypeScope() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide class Dep
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = inject<injectables.Dep>()
          """
        )
      )
    )
  )

  @Test fun testClassCompanionTypeScope() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            class Dep {
              companion object {
                @Provide val dep = Dep()
              }
            }
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = inject<injectables.Dep>()
          """
        )
      )
    )
  )

  @Test fun testTagTypeScope() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Tag annotation class MyTag {
              companion object {
                @Provide val default: @MyTag String = ""
              }
            }
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = inject<@injectables.MyTag String>()
          """
        )
      )
    )
  )

  @Test fun testTypeArgumentTypeScope() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            class Dep {
              companion object {
                @Provide val setOfDeps = setOf(Dep())
              }
            }
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = inject<Set<injectables.Dep>>()
          """
        )
      )
    )
  )

  @Test fun testSuperTypeTypeScope() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            abstract class AbstractDep {
              companion object {
                @Provide val dep = Dep()
              }
            }
            class Dep : AbstractDep()
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = inject<injectables.Dep>()
          """
        )
      )
    )
  )

  @Test fun testPackageTypeScope() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            class Dep
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        source(
          """
            @Provide val dep = Dep()
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = inject<injectables.Dep>()
          """
        )
      )
    )
  )

  @Test fun testPackageNestedClassTypeScope() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            class Dep

            object DepModule {
              @Provide val dep = Dep() 
            }
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = inject<injectables.Dep>()
          """
        )
      )
    )
  )

  @Test fun testTypeScopeWhichReferencesTypeInInjectableDeclaration() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Tag annotation class MyTag {
              companion object {
                @Provide inline fun <@Spread T : @MyTag S, S> value(t: T): S = t
              }
            }
          """,
          packageFqName = FqName("tags")
        )
      ),
      listOf(
        source(
          """
            class Dep {
              companion object {
                @Provide fun dep(): @tags.MyTag Dep = Dep()
              }
            }
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        invokableSource(
          """
            fun invoke() = inject<injectables.Dep>()
          """
        )
      )
    )
  )

  @Test fun testClassTypeScopeWithSpreadingInjectables() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Tag annotation class MyTag {
              companion object {
                @Provide inline fun <@Spread T : @MyTag S, S> value(t: T): S = t
              }
            }

            @MyTag @Provide class Dep
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        invokableSource(
          """
            fun invoke() = inject<injectables.Dep>()
          """
        )
      )
    )
  )

  @Test fun testTypeScopeCanAccessOtherTypeScope() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            fun interface AppTheme {
              operator fun invoke()
            }
            @Provide fun appTheme(): AppTheme = AppTheme {}
          """,
          packageFqName = FqName("package1")
        )
      ),
      listOf(
        source(
          """
            fun interface AppContent {
              operator fun invoke()
            }
            @Provide fun appContent(appTheme: package1.AppTheme): AppContent = AppContent {}
          """,
          packageFqName = FqName("package2")
        )
      ),
      listOf(
        invokableSource(
          """
            fun invoke() = inject<package2.AppContent>()
          """
        )
      )
    )
  )
}
