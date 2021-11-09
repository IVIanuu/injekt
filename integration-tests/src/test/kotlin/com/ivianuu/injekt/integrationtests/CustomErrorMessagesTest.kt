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

import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.singleAndMultiCodegen
import org.junit.Test

class CustomErrorMessagesTest {
  @Test fun testInjectableNotFoundOnClass() = singleAndMultiCodegen(
    """
      @InjectableNotFound("custom message [T]")
      class Dep<T>
    """,
    """
      fun invoke() = inject<Dep<String>>() 
    """
  ) {
    compilationShouldHaveFailed("custom message kotlin.String")
  }

  @Test fun testInjectableNotFoundOnSuperType() = singleAndMultiCodegen(
    """
      @InjectableNotFound("custom message [T]")
      interface DepInterface<T>

      class Dep<T> : DepInterface<T>
    """,
    """
      fun invoke() = inject<Dep<String>>() 
    """
  ) {
    compilationShouldHaveFailed("custom message kotlin.String")
  }

  @Test fun testInjectableNotFoundOnTag() = singleAndMultiCodegen(
    """
      @InjectableNotFound("custom message [T] [${"\\$"}TT]")
      @Tag annotation class MyTag<T>
    """,
    """
      fun invoke() = inject<@MyTag<String> Foo>() 
    """
  ) {
    compilationShouldHaveFailed("custom message kotlin.String com.ivianuu.injekt.test.Foo")
  }

  @Test fun testInjectableNotFoundOnParameter() = singleAndMultiCodegen(
    """
      fun <T> func(@Inject @InjectableNotFound("custom message [T]") value: T) {
      }
    """,
    """
      fun invoke() = func<String>()
    """
  ) {
    compilationShouldHaveFailed("custom message kotlin.String")
  }

  @Test fun testInjectableAmbiguous() = singleAndMultiCodegen(
    """
      @AmbiguousInjectable("custom message [T]")
      @Provide fun <T> amb1(): T = TODO()
      @Provide fun <T> amb2(): T = TODO()
    """,
    """
      fun invoke() = inject<String>() 
    """
  ) {
    compilationShouldHaveFailed("custom message kotlin.String")
  }
}
