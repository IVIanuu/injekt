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
