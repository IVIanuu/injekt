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

class GivenResolveTest {
  @Test fun testResolvesExternalGivenInSamePackage() = singleAndMultiCodegen(
    """
      @Given val foo = Foo()
    """,
    """
      fun invoke() = inject<Foo>()
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesExternalGivenInDifferentPackage() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Given val foo = Foo()
          """,
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        source(
          """
            @GivenImports("givens.*")
            fun invoke() = inject<Foo>()
          """,
          name = "File.kt"
        )
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesInternalGivenFromDifferentPackageWithAllUnderImport() = codegen(
    listOf(
      source(
        """
          @Given val foo = Foo()
        """,
        packageFqName = FqName("givens")
      ),
      source(
        """
          @GivenImports("givens.*")
          fun invoke() = inject<Foo>()
        """,
        name = "File.kt"
      )
    )
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesGivenInSamePackageAndSameFile() = codegen(
    """
      @Given val foo = Foo()
      fun invoke() = inject<Foo>()
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesClassCompanionGivenFromWithinTheClass() = singleAndMultiCodegen(
    """
      class MyClass {
        fun resolve() = inject<Foo>()
        companion object {
          @Given val foo = Foo()
        }
      }
    """,
    """
      fun invoke() = MyClass().resolve() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesClassCompanionGivenFromOuterClass() = singleAndMultiCodegen(
    """
      class MyClass {
        companion object {
          @Given val foo = Foo()
        }
      }
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesClassCompanionClassGivenFromOuterClass() = singleAndMultiCodegen(
    """
      class MyClass {
        companion object {
          @Given class MyModule {
            @Given val foo = Foo()
          }
        }
      }
    """,
    """
        fun invoke() = inject<Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesClassConstructorGiven() = singleAndMultiCodegen(
    """
      class MyClass(@Given val foo: Foo = Foo()) {
        fun resolve() = inject<Foo>()
      }
    """,
    """
      fun invoke() = MyClass().resolve() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testResolvesClassGiven() = singleAndMultiCodegen(
    """
      class MyClass {
        @Given val foo = Foo()
        fun resolve() = inject<Foo>()
      }
    """,
    """
      fun invoke() = MyClass().resolve() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testDerivedGiven() = singleAndMultiCodegen(
    """
      @Given val foo = Foo()
      @Given val bar: Bar = Bar(inject())
    """,
    """
      fun invoke() = inject<Bar>() 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Bar>()
  }

  @Test fun testCanResolveSubTypeOfGiven() = singleAndMultiCodegen(
    """
      interface Repo
      @Given class RepoImpl : Repo
    """,
    """
      fun invoke() = inject<Repo>() 
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testUnresolvedGiven() = codegen(
    """
      fun invoke() {
        inject<String>()
      }
    """
  ) {
    compilationShouldHaveFailed("no given argument found of type kotlin.String")
  }

  @Test fun testNestedUnresolvedGiven() = singleAndMultiCodegen(
    """
      @Given fun bar(foo: Foo) = Bar(foo)
    """,
    """
      fun invoke() = inject<Bar>() 
    """
  ) {
    compilationShouldHaveFailed(" no given argument found of type com.ivianuu.injekt.test.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.bar")
  }

  @Test fun testGenericGiven() = singleAndMultiCodegen(
    """
      @Given val foo = Foo()
      @Given fun <T> givenList(value: T): List<T> = listOf(value)
    """,
    """
      fun invoke() = inject<List<Foo>>() 
    """
  ) {
    val (foo) = invokeSingleFile<List<Any>>()
    foo.shouldBeTypeOf<Foo>()
  }

  @Test fun testFunctionInvocationWithGivens() = singleAndMultiCodegen(
    """
      @Given val foo = Foo()
      fun usesFoo(@Given foo: Foo) {
      }
    """,
    """
      fun invoke() {
        usesFoo()
      } 
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testLocalFunctionInvocationWithGivens() = codegen(
    """
      @Given val foo = Foo()
      fun invoke() {
        fun usesFoo(@Given foo: Foo) {
        }                    
        usesFoo()
      }
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testConstructorInvocationWithGivens() = singleAndMultiCodegen(
    """
      @Given val foo = Foo()
      class UsesFoo(@Given foo: Foo)
    """,
    """
      fun invoke() {
        UsesFoo()
      } 
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testPrimaryConstructorGivenWithReceiver() = singleAndMultiCodegen(
    """
      class UsesFoo(@Given val foo: Foo)
    """,
    """
      fun invoke(foo: Foo) = with(UsesFoo(foo)) {
        inject<Foo>()
      }
    """.trimIndent()
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testLocalConstructorInvocationWithGivens() = codegen(
    """
      @Given val foo = Foo()
      fun invoke() {
        class UsesFoo(@Given foo: Foo)
        UsesFoo()
      }
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testCanResolveGivenOfGivenThisFunction() = codegen(
    """
      class Dep(@Given val foo: Foo)
      fun invoke(foo: Foo) = with(Dep(foo)) { inject<Foo>() }
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testCanResolveGivenWhichDependsOnAssistedGivenOfTheSameType() = singleAndMultiCodegen(
    """
      typealias SpecialScope = Unit
      
      @Given fun <E> asRunnable(factory: (@Given SpecialScope) -> List<E>): List<E> = factory(Unit)
      
      @Given fun raw(scope: SpecialScope): List<String> = listOf("")
    """,
    """
      fun invoke() = inject<List<String>>()
    """
  )

  @Test fun testCanResolveStarProjectedType() = singleAndMultiCodegen(
    """
      @Given fun foos() = Foo() to Foo()
      
      @Qualifier annotation class First
      @Given fun <A : @First B, B> first(pair: Pair<B, *>): A = pair.first as A
    """,
    """
      fun invoke() = inject<@First Foo>() 
    """
  )

  @Test fun testCannotResolveObjectWithoutGiven() = singleAndMultiCodegen(
    """
      object MyObject
    """,
    """
      fun invoke() = inject<MyObject>() 
    """
  ) {
    compilationShouldHaveFailed("no given argument")
  }

  @Test fun testCanResolveObjectWithGiven() = singleAndMultiCodegen(
    """
      @Given object MyObject
    """,
    """
      fun invoke() = inject<MyObject>() 
    """
  )

  @Test fun testCannotResolveExternalInternalGiven() = multiCodegen(
    """
      @Given internal val foo = Foo()
    """,
    """
     fun invoke() = inject<Foo>()
    """
  ) {
    compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.test.Foo")
  }

  @Test fun testCannotResolvePrivateGivenFromOuterScope() = singleAndMultiCodegen(
    """
      @Given class FooHolder {
        @Given private val foo = Foo()
      }
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.test.Foo")
  }

  @Test fun testCanResolvePrivateGivenFromInnerScope() = codegen(
    """
      @Given class FooHolder {
        @Given private val foo = Foo()
        fun invoke() = inject<Foo>()
      }
    """
  )

  @Test fun testCannotResolveProtectedGivenFromOuterScope() = singleAndMultiCodegen(
    """
      @Given open class FooHolder {
        @Given protected val foo = Foo()
      }
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.test.Foo")
  }

  @Test fun testCanResolveProtectedGivenFromSameClass() = codegen(
    """
      @Given open class FooHolder {
        @Given protected val foo = Foo()
        fun invoke() = inject<Foo>()
      }
    """
  )

  @Test fun testCanResolveProtectedGivenFromSubClass() = singleAndMultiCodegen(
    """
      abstract class AbstractFooHolder {
        @Given protected val foo = Foo()
      }
    """,
    """
      class FooHolderImpl : AbstractFooHolder() {
        fun invoke() = inject<Foo>()
      } 
    """
  )

  @Test fun testCannotResolvePropertyWithTheSameNameAsAnGivenPrimaryConstructorParameter() =
    singleAndMultiCodegen(
      """
        @Given class MyClass(foo: Foo) {
          val foo = foo
        }
      """,
      """
        fun invoke() = inject<Foo>() 
      """
    ) {
      compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.inject")
    }

  @Test fun testCanResolveExplicitMarkedGivenConstructorParameterFromOutsideTheClass() =
    singleAndMultiCodegen(
      """
        class MyClass(@Given val foo: Foo)
      """,
      """
        fun invoke(@Given myClass: MyClass) = inject<Foo>() 
      """
    )

  @Test fun testCannotResolveImplicitGivenConstructorParameterFromOutsideTheClass() =
    singleAndMultiCodegen(
      """
        @Given class MyClass(val foo: Foo)
      """,
      """
        fun invoke() = inject<Foo>() 
      """
    ) {
      compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.inject")
    }

  @Test fun testCanResolveImplicitGivenConstructorParameterFromInsideTheClass() = codegen(
    """
      @Given class MyClass(private val foo: Foo) {
        fun invoke() = inject<Foo>()
      }
    """
  )

  @Test fun testResolvesGivenWithTypeParameterInScope() = singleAndMultiCodegen(
    """
      @Given fun <T> list(): List<T> = emptyList()
    """,
    """
      fun <T> invoke() {
        inject<List<T>>()
      } 
    """
  )

  @Test fun testCannotUseNonReifiedTypeParameterForReifiedGiven() = singleAndMultiCodegen(
    """
      @Given inline fun <reified T> list(): List<T> {
        T::class
        return emptyList()
      }
    """,
    """
      fun <T> invoke() {
        inject<List<T>>()
      }
    """
  ) {
    compilationShouldHaveFailed(
      "type parameter T of given argument com.ivianuu.injekt.integrationtests.list() of type kotlin.collections.List<com.ivianuu.injekt.integrationtests.invoke.T> for parameter value of function com.ivianuu.injekt.inject is marked with reified but type argument com.ivianuu.injekt.integrationtests.invoke.T is not marked with reified"
    )
  }

  @Test fun testCannotUseNonForTypeKeyTypeParameterForForTypeKeyGiven() = singleAndMultiCodegen(
    """
      @Given fun <@ForTypeKey T> list(): List<T> {
        typeKeyOf<T>()
        return emptyList()
      }
    """,
    """
      fun <T> invoke() {
        inject<List<T>>()
      } 
    """
  ) {
    compilationShouldHaveFailed(
      "type parameter T of given argument com.ivianuu.injekt.integrationtests.list() of type kotlin.collections.List<com.ivianuu.injekt.integrationtests.invoke.T> for parameter value of function com.ivianuu.injekt.inject is marked with @ForTypeKey but type argument com.ivianuu.injekt.integrationtests.invoke.T is not marked with @ForTypeKey"
    )
  }

  @Test fun testCannotResolveUnparameterizedSubTypeOfParameterizedGiven() = singleAndMultiCodegen(
    """
      typealias TypedString<T> = String
  
      @Given val foo = Foo()
  
      @Given fun <T : Foo> typedString(value: T): TypedString<T> = ""
    """,
    """
      fun invoke() = inject<String>() 
    """
  )

  @Test fun testCannotResolveUnparameterizedSubTypeOfParameterizedGivenWithQualifiers() =
    singleAndMultiCodegen(
      """
        typealias TypedString<T> = String

        @Given val foo = Foo()

        @Given fun <T : Foo> typedString(value: T): @TypedQualifier<T> TypedString<T> = ""
    """,
    """
      fun invoke() = inject<@TypedQualifier<Foo> String>() 
    """
    )
}