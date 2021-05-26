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
      @Provide class Dep(value: MyAlias)
      typealias MyAlias = String
      @Provide val myAlias: MyAlias = ""
    """,
    """
      fun invoke() = inject<Dep>()
    """
  )

  @Test fun testSecondaryConstructorWithTypeAliasDependencyMulti() = singleAndMultiCodegen(
    """
      class Dep {
        @Provide constructor(value: MyAlias)
      }
      typealias MyAlias = String
      @Provide val myAlias: MyAlias = ""
    """,
    """
      fun invoke() = inject<Dep>()
    """
  )

  @Test fun testModuleDispatchReceiverTypeInference() = singleAndMultiCodegen(
    """
      class MyModule<T : S, S> {
        @Provide fun provide(value: S): T = value as T
      }
  
      @Provide val module = MyModule<String, CharSequence>()
  
      @Provide val value: CharSequence = "42"
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
      @Provide fun <@Spread T : @MyQualifier S, S : FuncA> impl(instance: T): @MyOtherQualifier S =
        instance
  
      typealias FuncA = suspend () -> Unit
      typealias FuncB = suspend () -> Boolean
  
      @Provide fun funcA(funcB: FuncB): @MyQualifier FuncA = { }
      @Provide fun funcB(): @MyQualifier FuncB = { true }
    """,
    """
      fun invoke() {
        inject<@MyOtherQualifier FuncA>()
        inject<@MyOtherQualifier FuncB>()
      } 
    """
  ) {
    shouldNotContainMessage("no injectable found of type com.ivianuu.injekt.integrationtests.MyOtherQualifier<com.ivianuu.injekt.integrationtests.FuncA> for parameter value of function com.ivianuu.injekt.inject")
    shouldContainMessage("no injectable found of type com.ivianuu.injekt.integrationtests.MyOtherQualifier<com.ivianuu.injekt.integrationtests.FuncB> for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testNonProvideFunctionWithInjectParameters() = singleAndMultiCodegen(
    """
      fun myFunction(
        @Inject scopeFactory: (@Provide @InstallElement<AppScope> Any) -> AppScope
      ): AppScope = TODO()
    """,
    """
      @Providers("com.ivianuu.injekt.common.*", "com.ivianuu.injekt.scope.*")
      fun invoke() = myFunction()
    """
  )

  @Test fun testNonInjectablePrimaryConstructorWithInjectableParameters() = singleAndMultiCodegen(
    """
      class MyClass(
        @Inject scopeFactory: (@Provide @InstallElement<AppScope> Any) -> AppScope
      )
    """,
    """
      @Providers("com.ivianuu.injekt.common.*", "com.ivianuu.injekt.scope.*")
      fun invoke() = MyClass()
    """
  )

  @Test fun testNonInjectableSecondaryConstructorWithInjectableParameters() = singleAndMultiCodegen(
    """
      class MyClass {
        constructor(
          @Inject scopeFactory: (@Provide @InstallElement<AppScope> Any) -> AppScope
        )
      }
    """,
    """
      @Providers("com.ivianuu.injekt.common.*", "com.ivianuu.injekt.scope.*")
      fun invoke() = MyClass()
    """
  )

  @Test fun testNonInjectableClassWithInjectableMembers() = singleAndMultiCodegen(
    """ 
      abstract class MyModule<T : S, S> {
        @Provide fun func(t: T): S = t
      }
      class MyModuleImpl<T> : MyModule<@Qualifier1 T, T>()
    """,
    """
      @Provide val myFooModule = MyModuleImpl<Foo>()
      @Provide val foo: @Qualifier1 Foo = Foo()
      fun invoke() = inject<Foo>()
        """
  )

  @Test fun testNonInjectableClassWithInjectableMembers2() = singleAndMultiCodegen(
    """ 
      abstract class MyAbstractChildScopeModule<P : Scope, T : Any, S : T> {
        @Provide fun factory(scopeFactory: S): @InstallElement<P> @ChildScopeFactory T = scopeFactory
      }
      
      class MyChildScopeModule1<P : Scope, P1, C : Scope> : MyAbstractChildScopeModule<P,
        (P1) -> C, (@Provide @InstallElement<C> P1) -> C>()
    """,
    """
      typealias TestScope1 = Scope
      typealias TestScope2 = Scope
      @Providers("com.ivianuu.injekt.common.*", "com.ivianuu.injekt.scope.*")
      fun invoke() {
        @Provide val childScopeModule = MyChildScopeModule1<TestScope1, String, TestScope2>()
        val parentScope = inject<TestScope1>()
        val childScope = parentScope.element<@ChildScopeFactory (String) -> TestScope2>()("42")
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
        (1..150).map { "@Inject p$it: MyAlias<T>" }.joinToString("\n,")
      }): String = ""
    """,
    """
      fun invoke() {
        withInstances("" as MyAlias<String>) { largeFunc<String>() }
      }
    """
  ) {
    invokeSingleFile()
  }
}
