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

  @Test fun testQualifierTypeScope() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Qualifier annotation class MyQualifier {
              companion object {
                @Provide val default: @MyQualifier String = ""
              }
            }
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = inject<@injectables.MyQualifier String>()
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

  @Test fun testClassQualifierScope() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Qualifier annotation class MyQualifier {
              companion object {
                @Provide val dep = injectables.Dep()
              }
            }
          """,
          packageFqName = FqName("qualifiers")
        ),
        source(
          """
            @Provide @qualifiers.MyQualifier class Dep
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
}
