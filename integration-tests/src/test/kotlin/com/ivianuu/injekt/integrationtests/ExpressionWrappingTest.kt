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
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.irShouldContain
import com.ivianuu.injekt.test.irShouldNotContain
import com.ivianuu.injekt.test.singleAndMultiCodegen
import org.junit.Test

class ExpressionWrappingTest {
  @Test fun testDoesFunctionWrapInjectableWithMultipleUsages() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)
      @Provide fun <T> pair(a: T, b: T): Pair<T, T> = a to b
    """,
    """
      fun invoke() = inject<Pair<Bar, Bar>>()
    """
  ) {
    irShouldContain(1, "bar(foo = ")
  }

  @Test fun testDoesFunctionWrapInjectableWithMultipleUsagesInDifferentScopes() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)
      @Provide fun <T> pair(a: T, b: () -> T): Pair<T, () -> T> = a to b
    """,
    """
      fun invoke() = inject<Pair<Bar, () -> Bar>>()
    """
  ) {
    irShouldContain(1, "bar(foo = ")
  }

  @Test fun testDoesNotFunctionWrapInjectableWithSingleUsage() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)
    """,
    """
      fun invoke() = inject<Bar>()
    """
  ) {
    irShouldNotContain("local fun <anonymous>(): Bar {")
  }

  @Test fun testDoesNotWrapInjectableWithMultipleUsagesButWithoutDependencies() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide fun <T> pair(a: T, b: T): Pair<T, T> = a to b
    """,
    """
      fun invoke() = inject<Pair<Foo, Foo>>()
    """
  ) {
    irShouldNotContain("local fun <anonymous>(): Foo {")
  }

  @Test fun testDoesFunctionWrapComponentWithMultipleUsages() = codegen(
    """
      @Provide val foo = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)
      @Provide interface BarComponent : Component {
        val bar: Bar
      }
      @Provide fun <T> pair(a: T, b: T): Pair<T, T> = a to b
      fun invoke() {
        inject<Pair<BarComponent, BarComponent>>()
      }
    """
  ) {
    irShouldContain(1, "local class BarComponentImpl")
  }

  @Test fun testFunctionWrapScopedInjectable() = codegen(
    """
      @Provide @Scoped<MyComponent> val foo = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)

      @Provide interface MyComponent : Component {
        val bar: Bar
        val bar2: Bar
      }

      fun invoke() {
        inject<MyComponent>()
      }
    """
  ) {
    irShouldContain(1, "fun function0(): Bar {")
  }

  @Test fun testFunctionWrapScopedInjectableWithoutDependencies() = codegen(
    """
      @Provide @Scoped<MyComponent> val foo: Foo = Foo()

      @Provide interface MyComponent : Component {
        val foo: Foo
        val foo2: Foo
      }

      fun invoke() {
        inject<MyComponent>()
      }
    """
  ) {
    irShouldContain(1, "fun function0(): Foo {")
  }

  @Test fun testSearchBetterName() = codegen(
    """
      interface Logger

      @Provide object AndroidLogger : Logger

      @Provide @Scoped<MyComponent> fun androidLogger(factory: () -> AndroidLogger): Logger =
        factory()

      @Provide interface MyComponent : Component {
        val loggerFactory: () -> Logger
        val loggerFactory2: () -> Logger
      }

      fun invoke() {
        inject<MyComponent>()
      }
    """
  )

  @Test fun testDoesFunctionWrapListInjectablesWithSameElements() = singleAndMultiCodegen(
    """
      @Provide val a = "a"
      @Provide val b = "b"
      @Provide fun <T> pair(a: T, b: () -> T): Pair<T, () -> T> = a to b
    """,
    """
      fun invoke() = inject<Pair<List<String>, () -> List<String>>>()
    """
  ) {
    irShouldContain(1, "local fun function0(): List<String> {")
  }

  @Test fun testDoesFunctionWrapProviderWithMultipleUsages() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide fun <T> pair(a: T, b: T): Pair<T, T> = a to b
    """,
    """
      fun invoke() = inject<Pair<() -> Foo, () -> Foo>>()
    """
  ) {
    irShouldContain(1, "local fun function0(): Function0<Foo>")
  }

  @Test fun testDoesNotFunctionWrapProviderWithSingleUsage() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
    """,
    """
      fun invoke() = inject<() -> Foo>()
    """
  ) {
    irShouldNotContain("local fun function0(): Function0<Foo>")
  }

  @Test fun testDoesNotFunctionWrapInlineProvider() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide inline fun bar(fooProvider: () -> Foo) = Bar(fooProvider())
      @Provide fun <T> pair(a: T, b: T): Pair<T, T> = a to b
    """,
    """
      fun invoke() = inject<Pair<Bar, Bar>>()
    """
  ) {
    irShouldNotContain("local fun function0(): Function0<Foo>")
  }

  @Test fun testDoesNotFunctionWrapCircularDependency() = singleAndMultiCodegen(
    """
      @Provide class A(b: B)
      @Provide class B(a: () -> A, a2: () -> A)
     """,
    """
      fun invoke() = inject<B>() 
    """
  ) {
    irShouldNotContain("local fun function0(): Function0<Foo>")
    invokeSingleFile()
  }
}
