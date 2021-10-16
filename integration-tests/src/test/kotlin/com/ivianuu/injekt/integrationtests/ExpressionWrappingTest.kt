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

  @Test fun testDoesCacheProviderWithMultipleUsages() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide fun <T> pair(a: T, b: T): Pair<T, T> = a to b
    """,
    """
      fun invoke() = inject<Pair<() -> Foo, () -> Foo>>()
    """
  ) {
    irShouldNotContain("local fun <anonymous>(): Function0<Foo> {")
    irShouldContain(1, "val tmp0_0: Function0<Foo> = local fun <anonymous>(): Foo {")
  }

  @Test fun testDoesNotCacheProviderWithSingleUsage() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
    """,
    """
      fun invoke() = inject<() -> Foo>()
    """
  ) {
    irShouldNotContain("val tmp0_0: Function0<Foo> = local fun <anonymous>(): Foo {")
  }

  @Test fun testDoesNotCacheInlineProvider() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide inline fun bar(fooProvider: () -> Foo) = Bar(fooProvider())
      @Provide fun <T> pair(a: T, b: T): Pair<T, T> = a to b
    """,
    """
      fun invoke() = inject<Pair<Bar, Bar>>()
    """
  ) {
    irShouldNotContain("val tmp0_0: Function0<Foo> = local fun <anonymous>(): Foo {")
  }

  @Test fun testDoesNotCacheCircularDependency() = singleAndMultiCodegen(
    """
      @Provide class A(b: B)
      @Provide class B(a: () -> A, a2: () -> A)
     """,
    """
      fun invoke() = inject<B>() 
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testDoesFunctionWrapComponentWithMultipleUsages() = codegen(
    """
      @Provide val foo = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)
      @Component interface BarComponent {
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
      @Provide val foo: @Scoped<MyComponent> Foo = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)

      @Component interface MyComponent {
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
      @Provide val foo: @Scoped<MyComponent> Foo = Foo()

      @Component interface MyComponent {
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

      @Provide fun androidLogger(factory: () -> AndroidLogger): @Scoped<MyComponent> Logger =
        factory()

      @Component interface MyComponent {
        val loggerFactory: () -> Logger
        val loggerFactory2: () -> Logger
      }

      fun invoke() {
        inject<MyComponent>()
      }
    """
  )
}
