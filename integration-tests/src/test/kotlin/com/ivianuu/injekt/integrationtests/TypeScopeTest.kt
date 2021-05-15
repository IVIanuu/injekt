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
            class Dep {
              companion object {
                @Given val dep = Dep()
              }
            }
          """,
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = given<givens.Dep>()
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
              @Given val defaultDep = this
            }
          """,
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = given<givens.Dep>()
          """
        )
      )
    )
  )

  @Test fun testTypeAliasTypeScope() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            typealias Dep = String
            object DepGivens {
              @Given val default: Dep = ""
            }
          """,
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = given<givens.Dep>()
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
                @Given val default: @MyQualifier String = ""
              }
            }
          """,
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = given<@givens.MyQualifier String>()
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
                @Given val listOfDeps = listOf(Dep())
              }
            }
          """,
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = given<List<givens.Dep>>()
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
                @Given val dep = Dep()
              }
            }
            class Dep : AbstractDep()
          """,
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = given<givens.Dep>()
          """
        )
      )
    )
  )
}
