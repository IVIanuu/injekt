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
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = inject<givens.Dep>()
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
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = inject<givens.Dep>()
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
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = inject<givens.Dep>()
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
              @Provide val default: Dep = ""
            }
          """,
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = inject<givens.Dep>()
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
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = inject<@Provides.MyQualifier String>()
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
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = inject<List<givens.Dep>>()
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
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = inject<givens.Dep>()
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
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = inject<givens.Dep>()
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
                @Provide val dep = givens.Dep()
              }
            }
          """,
          packageFqName = FqName("qualifiers")
        ),
        source(
          """
            @Provide @qualifiers.MyQualifier class Dep
          """,
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = inject<givens.Dep>()
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
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = inject<givens.Dep>()
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
          @Provide val dep = Dep()
        """,
        packageFqName = FqName("givens")
      ),
      source(
        """
          fun invoke() = inject<givens.Dep>()
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
            @Provide val dep = Dep()
          """,
          packageFqName = FqName("givens")
        ),
        source(
          """
            fun invoke() = inject<givens.Dep>()
          """
        )
      )
    )
  ) {
    compilationShouldHaveFailed("no given argument found of type givens.Dep for parameter value of function com.ivianuu.injekt.inject")
  }
}
