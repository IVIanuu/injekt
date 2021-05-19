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
import io.kotest.matchers.nulls.*
import io.kotest.matchers.types.*
import org.junit.*

class ProviderTest {
  @Test fun testProviderInjectable() = singleAndMultiCodegen(
    """
            @Provide val foo = Foo()
    """,
    """
         fun invoke(): Foo {
                return inject<() -> Foo>()()
            } 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testCannotRequestProviderForNonExistingInjectable() = codegen(
    """ 
         fun invoke(): Foo {
                return inject<() -> Foo>()()
            }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type kotlin.Function0<com.ivianuu.injekt.test.Foo> for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testProviderWithInjectableArgs() = codegen(
    """
            @Provide fun bar(foo: Foo) = Bar(foo)
    """,
    """
        fun invoke() = inject<(@Provide Foo) -> Bar>()(Foo()) 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Bar>()
  }

  @Test fun testProviderWithQualifiedInjectableArgs() = singleAndMultiCodegen(
    """
      @Qualifier annotation class MyQualifier
      @Provide fun bar(foo: @MyQualifier Foo) = Bar(foo)
    """,
    """
      fun invoke() = inject<(@Provide @MyQualifier Foo) -> Bar>()(Foo()) 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Bar>()
  }

  @Test fun testProviderWithGenericInjectableArgs() = singleAndMultiCodegen(
    """ 
      typealias ScopeA = Scope

      typealias ScopeB = Scope

      @Provide fun scopeBFactory(
        parent: ScopeA,
        scopeFactory: () -> ScopeB
      ): @InstallElement<ScopeA> () -> ScopeB = scopeFactory

      typealias ScopeC = Scope

      @Provide fun scopeCFactory(
        parent: ScopeB,
        scopeFactory: () -> ScopeC
      ): @InstallElement<ScopeB> () -> ScopeC = scopeFactory
    """,
    """
      @Providers("com.ivianuu.injekt.common.*", "com.ivianuu.injekt.scope.*")
      fun createScopeA() = inject<ScopeA>()

      @InstallElement<ScopeC>
      @Provide 
      class MyComponent(
        val a: ScopeA,
        val b: ScopeB,
        val c: ScopeC
      )
    """
  )

  @Test fun testProviderModule() = singleAndMultiCodegen(
    """
      @Provide fun bar(foo: Foo) = Bar(foo)
      class FooModule(@Provide val foo: Foo)
    """,
    """
      fun invoke() = inject<(@Provide FooModule) -> Bar>()(FooModule(Foo()))
    """
  )

  @Test fun testSuspendProviderInjectable() = singleAndMultiCodegen(
    """
            @Provide suspend fun foo() = Foo()
    """,
    """
        fun invoke(): Foo = runBlocking { inject<suspend () -> Foo>()() } 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testComposableProviderInjectable() = singleAndMultiCodegen(
    """
            @Provide val foo: Foo @Composable get() = Foo()
    """,
    """
        fun invoke() = inject<@Composable () -> Foo>() 
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testMultipleProvidersInSetWithDependencyDerivedByProviderArgument() =
    singleAndMultiCodegen(
      """
        typealias MyScope = Scope
        @Provide val MyScope.key: String get() = ""
        @Provide fun foo(key: String) = Foo()
        @Provide fun fooIntoSet(provider: (@Provide MyScope) -> Foo): (MyScope) -> Any = provider as (MyScope) -> Any 
        @Provide class Dep(key: String)
        @Provide fun depIntoSet(provider: (@Provide MyScope) -> Dep): (MyScope) -> Any = provider as (MyScope) -> Any
    """,
      """
        fun invoke() {
          inject<Set<(MyScope) -> Any>>()
        } 
    """
    )

  @Test fun testProviderWhichReturnsItsParameter() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
    """,
    """
      fun invoke() = inject<(@Provide Foo) -> Foo>()(Foo())
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testProviderWithoutCandidatesError() = codegen(
    """
         fun invoke() {
                inject<() -> Foo>()
            }
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type kotlin.Function0<com.ivianuu.injekt.test.Foo> for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testProviderWithNullableReturnTypeUsesNullAsDefault() = codegen(
    """
         fun invoke() = inject<() -> Foo?>()()
    """
  ) {
    invokeSingleFile().shouldBeNull()
  }

  @Test fun testProviderWithNullableReturnTypeAndDefaultOnAllErrors() = codegen(
    """
            @Provide fun bar(foo: Foo) = Bar(foo)
         fun invoke() = inject<@DefaultOnAllErrors () -> Bar?>()()
    """
  ) {
    invokeSingleFile().shouldBeNull()
  }
}