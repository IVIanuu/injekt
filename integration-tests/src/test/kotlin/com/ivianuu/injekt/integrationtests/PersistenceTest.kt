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

import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.shouldContainMessage
import com.ivianuu.injekt.test.shouldNotContainMessage
import com.ivianuu.injekt.test.singleAndMultiCodegen
import io.kotest.matchers.shouldBe
import org.junit.Test

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
    invokeSingleFile() shouldBe "42"
  }

  @Test fun testFunctionTypeParameterClassifier() = singleAndMultiCodegen(
    """
      var callCount = 0
      @Tag annotation class MyTag
      @Tag annotation class MyOtherTag
      @Provide fun <@Spread T : @MyTag S, S : FuncA> impl(instance: T): @MyOtherTag S =
        instance
  
      typealias FuncA = suspend () -> Unit
      typealias FuncB = suspend () -> Boolean
  
      @Provide fun funcA(funcB: FuncB): @MyTag FuncA = { }
      @Provide fun funcB(): @MyTag FuncB = { true }
    """,
    """
      fun invoke() {
        inject<@MyOtherTag FuncA>()
        inject<@MyOtherTag FuncB>()
      } 
    """
  ) {
    shouldNotContainMessage("no injectable found of type com.ivianuu.injekt.integrationtests.MyOtherTag<com.ivianuu.injekt.integrationtests.FuncA> for parameter value of function com.ivianuu.injekt.inject")
    shouldContainMessage("no injectable found of type com.ivianuu.injekt.integrationtests.MyOtherTag<com.ivianuu.injekt.integrationtests.FuncB> for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testNonProvideFunctionWithInjectParameters() = singleAndMultiCodegen(
    """
      fun myFunction(@Inject unit: Unit) {
      }
    """,
    """ 
      fun invoke(@Provide unit: Unit) = myFunction()
    """
  )

  @Test fun testNonInjectablePrimaryConstructorWithInjectableParameters() = singleAndMultiCodegen(
    """
      class MyClass(@Inject unit: Unit)
    """,
    """
      fun invoke(@Inject unit: Unit) = MyClass()
    """
  )

  @Test fun testNonInjectableSecondaryConstructorWithInjectableParameters() = singleAndMultiCodegen(
    """
      class MyClass {
        constructor(@Inject unit: Unit)
      }
    """,
    """
      fun invoke(@Provide unit: Unit) = MyClass()
    """
  )

  @Test fun testNonInjectableClassWithInjectableMembers() = singleAndMultiCodegen(
    """ 
      abstract class MyModule<T : S, S> {
        @Provide fun func(t: T): S = t
      }
      class MyModuleImpl<T> : MyModule<@Tag1 T, T>()
    """,
    """
      @Provide val myFooModule = MyModuleImpl<Foo>()
      @Provide val foo: @Tag1 Foo = Foo()
      fun invoke() = inject<Foo>()
        """
  )

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
        with("" as MyAlias<String>) { largeFunc<String>() }
      }
    """
  ) {
    invokeSingleFile()
  }
}
