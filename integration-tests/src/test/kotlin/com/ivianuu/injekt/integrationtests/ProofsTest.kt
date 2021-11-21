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

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import org.junit.Test

class ProofsTest {
  @Test fun testNotProvidedTrue() = codegen(
    """
      fun foo(@Inject ev: NotProvided<Bar>) = Foo()

      fun invoke() = foo()
    """
  )

  @Test fun testNotProvidedFalse() = codegen(
    """
      @Provide fun bar() = Bar(Foo())
      fun foo(@Inject ev: NotProvided<Bar>) = Foo()

      fun invoke() = foo()
    """
  ) {
    compilationShouldHaveFailed()
  }

  @Test fun testIsSubTypeTrue() = codegen(
    """
      fun <T> foo(@Inject ev: IsSubType<T, CharSequence>) = Foo()

      fun invoke() = foo<String>()
    """
  )

  @Test fun testIsSubTypeFalse() = codegen(
    """
      fun <T> foo(@Inject ev: IsSubType<T, CharSequence>) = Foo()

      fun invoke() = foo<Int>()
    """
  ) {
    compilationShouldHaveFailed()
  }

  @Test fun testIsNotSubTypeTrue() = codegen(
    """
      fun <T> foo(@Inject ev: IsNotSubType<T, CharSequence>) = Foo()

      fun invoke() = foo<Int>()
    """
  )

  @Test fun testIsNotSubTypeFalse() = codegen(
    """
      fun <T> foo(@Inject ev: IsNotSubType<T, CharSequence>) = Foo()

      fun invoke() = foo<String>()
    """
  ) {
    compilationShouldHaveFailed()
  }

  @Test fun testIsEqualTrue() = codegen(
    """
      fun <T> foo(@Inject ev: IsEqual<T, CharSequence>) = Foo()

      fun invoke() = foo<CharSequence>()
    """
  )

  @Test fun testIsEqualFalse() = codegen(
    """
      fun <T> foo(@Inject ev: IsEqual<T, CharSequence>) = Foo()

      fun invoke() = foo<String>()
    """
  ) {
    compilationShouldHaveFailed()
  }

  @Test fun testIsNotEqualTrue() = codegen(
    """
      fun <T> foo(@Inject ev: IsNotEqual<T, CharSequence>) = Foo()

      fun invoke() = foo<Int>()
    """
  )

  @Test fun testIsNotEqualFalse() = codegen(
    """
      fun <T> foo(@Inject ev: IsNotEqual<T, CharSequence>) = Foo()

      fun invoke() = foo<CharSequence>()
    """
  ) {
    compilationShouldHaveFailed()
  }

  @Test fun testInSuspendTrue() = codegen(
    """
      fun foo(@Inject ev: InSuspend) = Foo()

      suspend fun invoke() = foo()
    """
  )

  @Test fun testInSuspendFalse() = codegen(
    """
      fun foo(@Inject ev: InSuspend) = Foo()

      fun invoke() = foo()
    """
  ) {
    compilationShouldHaveFailed()
  }

  @Test fun testNotInSuspendTrue() = codegen(
    """
      fun foo(@Inject ev: NotInSuspend) = Foo()

      fun invoke() = foo()
    """
  )

  @Test fun testNotInSuspendFalse() = codegen(
    """
      fun foo(@Inject ev: NotInSuspend) = Foo()

      suspend fun invoke() = foo()
    """
  ) {
    compilationShouldHaveFailed()
  }

  @Test fun testReifiedTrue() = codegen(
    """
      fun <T> foo(@Inject ev: Reified<T>) = Foo()

      inline fun <reified T> invoke() = foo<T>()
    """
  )

  @Test fun testReifiedFalse() = codegen(
    """
      fun <T> foo(@Inject ev: Reified<T>) = Foo()

      fun <T> invoke() = foo<T>()
    """
  ) {
    compilationShouldHaveFailed()
  }

  @Test fun testNotReifiedTrue() = codegen(
    """
      fun <T> foo(@Inject ev: NotReified<T>) = Foo()

      fun <T> invoke() = foo<T>()
    """
  )

  @Test fun testNotReifiedFalse() = codegen(
    """
      fun <T> foo(@Inject ev: NotReified<T>) = Foo()

      inline fun <reified T> invoke() = foo<T>()
    """
  ) {
    compilationShouldHaveFailed()
  }

  @Test fun testInComponentTrue() = codegen(
    """
      @Provide interface MyComponent : Component {
        val foo: Foo
      }

      @Provide fun foo(ev: InComponent<MyComponent>) = Foo()

      fun invoke() = inject<MyComponent>()
    """
  )

  @Test fun testInComponentFalse() = codegen(
    """
      @Component interface MyComponent

      @Provide fun foo(ev: InComponent<MyComponent>) = Foo()

      fun invoke() = inject<Foo>()
    """
  ) {
    compilationShouldHaveFailed()
  }

  @Test fun testNotInComponentTrue() = codegen(
    """
      @Provide interface MyComponent : Component

      @Provide fun foo(ev: NotInComponent<MyComponent>) = Foo()

      fun invoke() = inject<Foo>()
    """
  )

  @Test fun testNotInComponentFalse() = codegen(
    """
      @Provide interface MyComponent : Component {
        val foo: Foo
      }

      @Provide fun foo(ev: NotInComponent<MyComponent>) = Foo()

      fun invoke() = inject<MyComponent>()
    """
  ) {
    compilationShouldHaveFailed()
  }
}
