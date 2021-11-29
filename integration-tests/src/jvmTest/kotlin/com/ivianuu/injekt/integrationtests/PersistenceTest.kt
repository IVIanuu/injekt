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
import com.ivianuu.injekt.test.singleAndMultiCodegen
import io.kotest.matchers.shouldBe
import org.junit.Test

class PersistenceTest {
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
      @Tag annotation class MyAlias<T>
      fun <T> largeFunc(${
        (1..150).map { "${if (it == 1) "@Inject " else ""}p$it: @MyAlias<T> Any?" }.joinToString("\n,")
      }): String = ""
    """,
    """
      fun invoke() {
        with("" as @MyAlias<String> String) { largeFunc<String>() }
      }
    """
  ) {
    invokeSingleFile()
  }
}
