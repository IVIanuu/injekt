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
import io.kotest.matchers.*
import org.junit.*

class PersistenceTest {
  @Test fun testPrimaryConstructorWithTypeAliasDependencyMulti() = singleAndMultiCodegen(
    """
      @Given class Dep(value: MyAlias)
      typealias MyAlias = String
      @Given val myAlias: MyAlias = ""
    """,
    """
      fun invoke() = inject<Dep>()
    """
  )

  @Test fun testSecondaryConstructorWithTypeAliasDependencyMulti() = singleAndMultiCodegen(
    """
      class Dep {
        @Given constructor(value: MyAlias)
      }
      typealias MyAlias = String
      @Given val myAlias: MyAlias = ""
    """,
    """
      fun invoke() = inject<Dep>()
    """
  )

  @Test fun testModuleDispatchReceiverTypeInference() = singleAndMultiCodegen(
    """
      class MyModule<T : S, S> {
        @Given fun provide(value: S): T = value as T
      }
  
      @Given val module = MyModule<String, CharSequence>()
  
      @Given val value: CharSequence = "42"
    """,
    """
      fun invoke() = inject<String>() 
    """
  ) {
    "42" shouldBe invokeSingleFile()
  }

  @Test fun testFunctionTypeParameterClassifier() = singleAndMultiCodegen(
    """
      var callCount = 0
      @Qualifier annotation class MyQualifier
      @Qualifier annotation class MyOtherQualifier
      @Given fun <@Spread T : @MyQualifier S, S : FuncA> impl(instance: T): @MyOtherQualifier S =
        instance
  
      typealias FuncA = suspend () -> Unit
      typealias FuncB = suspend () -> Boolean
  
      @Given fun funcA(funcB: FuncB): @MyQualifier FuncA = { }
      @Given fun funcB(): @MyQualifier FuncB = { true }
    """,
    """
      fun invoke() {
        inject<@MyOtherQualifier FuncA>()
        inject<@MyOtherQualifier FuncB>()
      } 
    """
  ) {
    shouldNotContainMessage("no given argument found of type com.ivianuu.injekt.integrationtests.MyOtherQualifier<com.ivianuu.injekt.integrationtests.FuncA> for parameter value of function com.ivianuu.injekt.inject")
    shouldContainMessage("no given argument found of type com.ivianuu.injekt.integrationtests.MyOtherQualifier<com.ivianuu.injekt.integrationtests.FuncB> for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testNonGivenFunctionWithGivenParameters() = singleAndMultiCodegen(
    """
      fun myFunction(
          @Given scopeFactory: (@Given @InstallElement<AppGivenScope> Any) -> AppGivenScope
      ): AppGivenScope = TODO()
    """,
    """
      @GivenImports("com.ivianuu.injekt.common.*", "com.ivianuu.injekt.scope.*")
      fun invoke() = myFunction()
    """
  )

  @Test fun testNonGivenPrimaryConstructorWithGivenParameters() = singleAndMultiCodegen(
    """
      class MyClass(
          @Given scopeFactory: (@Given @InstallElement<AppGivenScope> Any) -> AppGivenScope
      )
    """,
    """
      @GivenImports("com.ivianuu.injekt.common.*", "com.ivianuu.injekt.scope.*")
      fun invoke() = MyClass()
    """
  )

  @Test fun testNonGivenSecondaryConstructorWithGivenParameters() = singleAndMultiCodegen(
    """
      class MyClass {
          constructor(
            @Given scopeFactory: (@Given @InstallElement<AppGivenScope> Any) -> AppGivenScope
          )
      }
    """,
    """
      @GivenImports("com.ivianuu.injekt.common.*", "com.ivianuu.injekt.scope.*")
      fun invoke() = MyClass()
    """
  )

  @Test fun testNonGivenClassWithGivenMembers() = singleAndMultiCodegen(
    """ 
      abstract class MyModule<T : S, S> {
        @Given fun func(t: T): S = t
      }
      class MyModuleImpl<T> : MyModule<@Qualifier1 T, T>()
    """,
    """
      @Given val myFooModule = MyModuleImpl<Foo>()
      @Given val foo: @Qualifier1 Foo = Foo()
      fun invoke() = inject<Foo>()
        """
  )

  @Test fun testNonGivenClassWithGivenMembers2() = singleAndMultiCodegen(
    """ 
      abstract class MyAbstractChildGivenScopeModule<P : GivenScope, T : Any, S : T> {
        @Given fun factory(scopeFactory: S): @InstallElement<P> @ChildScopeFactory T = scopeFactory
      }
      
      class MyChildGivenScopeModule1<P : GivenScope, P1, C : GivenScope> : MyAbstractChildGivenScopeModule<P,
        (P1) -> C, (@Given @InstallElement<C> P1) -> C>()
    """,
    """
      typealias TestGivenScope1 = GivenScope
      typealias TestGivenScope2 = GivenScope
      @GivenImports("com.ivianuu.injekt.common.*", "com.ivianuu.injekt.scope.*")
      fun invoke() {
        @Given val childScopeModule = MyChildGivenScopeModule1<TestGivenScope1, String, TestGivenScope2>()
        val parentScope = inject<TestGivenScope1>()
        val childScope = parentScope.element<@ChildScopeFactory (String) -> TestGivenScope2>()("42")
        childScope.element<String>()
      }
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testSupportsLargeFunction() = singleAndMultiCodegen(
    """
      typealias MyAlias<T> = OtherAlias<T>
      typealias OtherAlias<S> = String
      fun <T> largeFunc(${
        (1..150).map { "@Given p$it: MyAlias<T>" }.joinToString("\n,")
      }): String = ""
    """,
    """
      fun invoke() {
        withGivens("" as MyAlias<String>) { largeFunc<String>() }
      }
    """
  ) {
    invokeSingleFile()
  }
}
