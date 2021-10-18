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
import com.ivianuu.injekt.test.invokeSingleFile
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

  @Test fun testTypeScopeWhichReferencesTypeInInjectableDeclaration() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            class UpCastable<T, D : T>(val value: T)
            typealias UpCasted<T> = T
            
            @Provide fun <D> upCastableImpl(t: UpCastable<*, D>): UpCasted<D> = t as D
          """,
          packageFqName = FqName("injectables1")
        )
      ),
      listOf(
        source(
          """
            class Dep {
              companion object {
                @Provide fun dep(): injectables1.UpCastable<Any, Dep> = injectables1.UpCastable(Dep())
              }
            }
          """,
          packageFqName = FqName("injectables2")
        )
      ),
      listOf(
        invokableSource(
          """
            fun invoke() = inject<injectables1.UpCasted<injectables2.Dep>>()
          """
        )
      )
    )
  )
}
