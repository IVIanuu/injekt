package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.*
import io.kotest.matchers.types.*
import org.jetbrains.kotlin.name.*
import org.junit.*

class TypeAliasGivensTest {
  @Test fun testImportingTypeAliasAlsoImportsItsGivensObject() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            typealias Dep = String
            object DepGivens {
              @Given val foo = Foo()
            }
          """,
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            @GivenImports("givens.Dep")
            fun invoke() = given<Foo>()
          """,
          name = "File.kt"
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun nonObjectTypeAliasGivens() = codegen(
    """
      typealias MyAlias = String

      class MyAliasGivens
    """
  ) {
    compilationShouldHaveFailed("typealias givens must be an object")
  }

  @Test fun typeAliasGivensInDifferentModule() = multiCodegen(
    """
      typealias MyAlias = String
    """,
    """
      object MyAliasGivens
    """
  ) {
    compilationShouldHaveFailed("typealias givens must be declared in the same compilation unit")
  }
}
