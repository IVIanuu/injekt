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

import com.ivianuu.injekt.test.invokableSource
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.multiPlatformCodegen
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

  @Test fun testTypeAliasModuleTypeScope() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            typealias Dep = String
            object DepModule {
              @Provide val default: Dep = ""
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

  @Test fun testTypeAliasPackageTypeScope() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            typealias Dep = String
            @Provide val dep: Dep = ""
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
                @Provide val listOfDeps = listOf(Dep())
              }
            }
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = inject<List<injectables.Dep>>()
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

  @Test fun testClassTagScope() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Tag annotation class MyTag {
              companion object {
                @Provide val dep = injectables.Dep()
              }
            }
          """,
          packageFqName = FqName("tags")
        ),
        source(
          """
            @Provide @tags.MyTag class Dep
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

  @Test fun testNestedClassTypeScopeWithSpreadingInjectables() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Tag annotation class MyTag2 {
              companion object {
                @Provide inline fun <@Spread T : @MyTag2 S, S> value(t: T): S = t
              }
            }
          """,
          packageFqName = FqName("tags2")
        )
      ),
      listOf(
        source(
          """
            @Tag annotation class MyTag1 {
              companion object {
                @Provide inline fun <@Spread T : @MyTag1 S, S> value(t: T): @tags2.MyTag2 S = t
              }
            }
          """,
          packageFqName = FqName("tags1")
        )
      ),
      listOf(
        source(
          """
            @tags1.MyTag1 @Provide class Dep
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
            typealias AppTheme = () -> Unit
            @Provide fun appTheme(): AppTheme = {}
          """,
          packageFqName = FqName("package1")
        )
      ),
      listOf(
        source(
          """
            typealias AppContent = () -> Unit
            @Provide fun appContent(appTheme: package1.AppTheme): AppContent = {}
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
