/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.*
import org.jetbrains.kotlin.name.*
import org.junit.*

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

  @Test fun testObjectTypeScope() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            object Dep {
              @Provide val defaultDep = this
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

  @Test fun testClassPackageTypeScope() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            interface Dep

            @Provide class DepImpl : Dep
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

            object DepImplicits {
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

  @Test fun testPackageDeeplyNestedClassTypeScope() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            class Dep

            object DepImplicits {
              class MyClass {
                object RealDepImplicits {
                  @Provide val dep = Dep()
                }
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

  @Test fun testMultiplatformPackageTypeScope() = multiPlatformCodegen(
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
      ),
      source(
        """
          fun invoke() = inject<injectables.Dep>()
        """
      )
    )
  )

  @Test fun testExternalModulesCanContributeToPackageTypeScope() = multiCodegen(
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
        ),
        source(
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

  @Test fun testTypeScopeRequestWithObjectImpl() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            interface MyType

            @Provide object MyTypeImpl : MyType
          """,
          packageFqName = FqName("package1")
        )
      ),
      listOf(
        invokableSource(
          """
            fun invoke() = inject<package1.MyType>()
          """
        )
      )
    )
  ) {
    invokeSingleFile()
  }
}
