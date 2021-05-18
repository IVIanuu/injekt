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
import org.junit.*

class ExpressionWrappingTest {
  @Test fun testDoesFunctionWrapGivenWithMultipleUsages() = singleAndMultiCodegen(
    """
      @Given val foo = Foo()
      @Given fun bar(foo: Foo) = Bar(foo)
      @Given fun <T> pair(a: T, b: T): Pair<T, T> = a to b
    """,
    """
      fun invoke() = summon<Pair<Bar, Bar>>()
    """
  ) {
    irShouldContain(1, "bar(foo = ")
  }

  @Test fun testDoesFunctionWrapGivenWithMultipleUsagesInDifferentScopes() = singleAndMultiCodegen(
    """
      @Given val foo = Foo()
      @Given fun bar(foo: Foo) = Bar(foo)
      @Given fun <T> pair(a: T, b: () -> T): Pair<T, () -> T> = a to b
    """,
    """
      fun invoke() = summon<Pair<Bar, () -> Bar>>()
    """
  ) {
    irShouldContain(1, "bar(foo = ")
  }

  @Test fun testDoesNotFunctionWrapGivenWithSingleUsage() = singleAndMultiCodegen(
    """
      @Given val foo = Foo()
      @Given fun bar(foo: Foo) = Bar(foo)
    """,
    """
      fun invoke() = summon<Bar>()
    """
  ) {
    irShouldNotContain("local fun <anonymous>(): Bar {")
  }

  @Test fun testDoesNotWrapGivenWithMultipleUsagesButWithoutDependencies() = singleAndMultiCodegen(
    """
      @Given val foo = Foo()
      @Given fun <T> pair(a: T, b: T): Pair<T, T> = a to b
    """,
    """
      fun invoke() = summon<Pair<Foo, Foo>>()
    """
  ) {
    irShouldNotContain("local fun <anonymous>(): Foo {")
  }

  @Test fun testDoesCacheProviderWithMultipleUsages() = singleAndMultiCodegen(
    """
      @Given val foo = Foo()
      @Given fun <T> pair(a: T, b: T): Pair<T, T> = a to b
    """,
    """
      fun invoke() = summon<Pair<() -> Foo, () -> Foo>>()
    """
  ) {
    irShouldNotContain("local fun <anonymous>(): Function0<Foo> {")
    irShouldContain(1, "val tmp0_0: Function0<Foo> = local fun <anonymous>(): Foo {")
  }

  @Test fun testDoesNotCacheProviderWithSingleUsage() = singleAndMultiCodegen(
    """
      @Given val foo = Foo()
    """,
    """
      fun invoke() = summon<() -> Foo>()
    """
  ) {
    irShouldNotContain("val tmp0_0: Function0<Foo> = local fun <anonymous>(): Foo {")
  }

  @Test fun testDoesNotCacheInlineProvider() = singleAndMultiCodegen(
    """
      @Given val foo = Foo()
      @Given inline fun bar(fooProvider: () -> Foo) = Bar(fooProvider())
      @Given fun <T> pair(a: T, b: T): Pair<T, T> = a to b
    """,
    """
      fun invoke() = summon<Pair<Bar, Bar>>()
    """
  ) {
    irShouldNotContain("val tmp0_0: Function0<Foo> = local fun <anonymous>(): Foo {")
  }

  @Test fun testDoesNotCacheCircularDependency() = singleAndMultiCodegen(
    """
      @Given class A(b: B)
      @Given class B(a: () -> A, a2: () -> A)
     """,
    """
      fun invoke() = summon<B>() 
    """
  ) {
    invokeSingleFile()
  }
}