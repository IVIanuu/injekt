/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.*
import io.kotest.matchers.collections.*
import org.jetbrains.kotlin.name.*
import org.junit.*

class DuplicatesTest {
  @Test fun testDoubleStarImportDoesNotProduceDuplicates() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide class Dep {
              @Provide fun unit() = Unit

              companion object {
                @Provide fun defaultElements(): Collection<ListElement> = listOf(ListElement())
              }
            }
        
            class ListElement
          """,
          packageFqName = FqName("package1")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("package1.**")
            fun invoke() = inject<List<package1.ListElement>>()
          """
        )
      )
    )
  ) {
    invokeSingleFile<List<*>>() shouldHaveSize 1
  }

  @Test fun testTypeScopeDoesNotProduceDuplicates() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            interface MyType

            @Provide object MyTypeImpl : MyType

            @Provide fun pair(type: MyType): Pair<String, MyType> = "a" to type
          """,
          packageFqName = FqName("package1")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("package1.*")
            fun invoke() = inject<List<Pair<String, package1.MyType>>>()
          """
        )
      )
    )
  ) {
    invokeSingleFile<List<*>>() shouldHaveSize 1
  }

  @Test fun testTypeScopeDoesNotProduceDuplicates2() = codegen(
    """
      fun invoke(): List<Element<*, *>> {
        class MyScope
      
        @Provide fun unit() = Unit
      
        @Provide fun unitElement(unit: Unit) = Element<MyScope, Unit>(unit)
      
        return inject<List<Element<*, *>>>()
      }
    """
  ) {
    invokeSingleFile<List<*>>() shouldHaveSize 1
  }

  @Test fun testInjectableChainingDoesNotProduceDuplicates() = singleAndMultiCodegen(
    """
      class Dep {
        companion object {
          @Provide fun element(): Unit = Unit
        }
      }
    """,
    """
      fun invoke() = inject<(Dep, Dep) -> List<Unit>>()(Dep(), Dep())
    """
  ) {
    invokeSingleFile<List<*>>() shouldHaveSize 1
  }
}
