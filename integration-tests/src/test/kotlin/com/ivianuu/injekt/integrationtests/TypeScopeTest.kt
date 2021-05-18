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
            @Given class Dep
          """,
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = summon<givens.Dep>()
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
            fun invoke() = summon<givens.Dep>()
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
            fun invoke() = summon<givens.Dep>()
          """
        )
      )
    )
  )

  @Test fun testTypeAliasGivensTypeScope() = singleAndMultiCodegen(
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
            fun invoke() = summon<givens.Dep>()
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
            fun invoke() = summon<@givens.MyQualifier String>()
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
            fun invoke() = summon<List<givens.Dep>>()
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
            fun invoke() = summon<givens.Dep>()
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

            @Given val dep = Dep()
          """,
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = summon<givens.Dep>()
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
                @Given val dep = givens.Dep()
              }
            }
          """,
          packageFqName = FqName("qualifiers")
        ),
        source(
          """
            @Given @qualifiers.MyQualifier class Dep
          """,
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = summon<givens.Dep>()
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
              @Given val dep = Dep() 
            }
          """,
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = summon<givens.Dep>()
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
        packageFqName = FqName("givens")
      )
    ),
    listOf(
      source(
        """
          @Given val dep = Dep()
        """,
        packageFqName = FqName("givens")
      ),
      source(
        """
          fun invoke() = summon<givens.Dep>()
        """
      )
    )
  )

  @Test fun testExternalDeclarationsDoNotContributeToPackageTypeScope() = multiCodegen(
    listOf(
      listOf(
        source(
          """
            class Dep
          """,
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            @Given val dep = Dep()
          """,
          packageFqName = FqName("givens")
        ),
        source(
          """
            fun invoke() = summon<givens.Dep>()
          """
        )
      )
    )
  ) {
    compilationShouldHaveFailed("no given argument found of type givens.Dep for parameter value of function com.ivianuu.injekt.summon")
  }
}
