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
import io.kotest.matchers.types.*
import org.jetbrains.kotlin.name.*
import org.junit.*

class TypeAliasModuleTest {
  @Test fun testImportingTypeAliasAlsoImportsItsModuleObject() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            typealias Dep = String
            object DepModule {
              @Provide val foo = Foo()
            }
          """,
          packageFqName = FqName("injectables")
        )
      ),
      listOf(
        source(
          """
            @Providers("injectables.Dep")
            fun invoke() = inject<Foo>()
          """,
          name = "File.kt"
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun nonObjectTypeAliasInjectables() = codegen(
    """
      typealias MyAlias = String

      class MyAliasModule
    """
  ) {
    compilationShouldHaveFailed("typealias module must be an object")
  }

  @Test fun typeAliasModuleInDifferentModule() = multiCodegen(
    """
      typealias MyAlias = String
    """,
    """
      object MyAliasModule
    """
  ) {
    compilationShouldHaveFailed("typealias module must be declared in the same compilation unit")
  }
}
