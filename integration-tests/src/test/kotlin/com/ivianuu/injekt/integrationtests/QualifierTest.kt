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
import io.kotest.matchers.types.*
import org.junit.*

class QualifierTest {
  @Test fun testDistinctQualifier() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide val qualifiedFoo: @Qualifier1 Foo = Foo()
        """,
    """
      fun invoke(): Pair<Foo, Foo> {
        return inject<Foo>() to inject<@Qualifier1 Foo>()
      } 
    """
  ) {
    val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
    foo1 shouldNotBeSameInstanceAs foo2
  }

  @Test fun testTypeParameterWithQualifierUpperBound() = singleAndMultiCodegen(
    """
      @Provide class Dep<T>(val value: @Qualifier1 T)
            
      @Provide fun qualified(): @Qualifier1 String = ""
    """,
    """
      fun invoke() = inject<Dep<String>>() 
    """
  )

  @Test fun testQualifiedClass() = singleAndMultiCodegen(
    """ 
      @Provide @Qualifier1 class Dep
    """,
    """
      fun invoke() = inject<@Qualifier1 Dep>()
    """
  )

  @Test fun testQualifiedPrimaryConstructor() = singleAndMultiCodegen(
    """ 
      class Dep @Provide @Qualifier1 constructor()
    """,
    """
      fun invoke() = inject<@Qualifier1 Dep>()
    """
  )

  @Test fun testQualifiedSecondaryConstructor() = singleAndMultiCodegen(
    """ 
      class Dep {
        @Provide @Qualifier1 constructor()
      }
        """,
    """
      fun invoke() = inject<@Qualifier1 Dep>()
    """
  )

  @Test fun testQualifiedObject() = singleAndMultiCodegen(
    """ 
      @Provide @Qualifier1 object Dep
    """,
    """
      fun invoke() = inject<@Qualifier1 Dep>()
    """
  )

  @Test fun testQualifiedFunction() = codegen(
    """ 
      @Provide @Qualifier1 fun foo() = Foo()
    """
  ) {
    compilationShouldHaveFailed("only types, classes and class constructors can be annotated with a qualifier")
  }

  @Test fun testQualifierWithArguments() = codegen(
    """ 
      @Qualifier annotation class MyQualifier(val value: String)
    """
  ) {
    compilationShouldHaveFailed("qualifier cannot have value parameters")
  }

  @Test fun testQualifierWithTypeArguments() = singleAndMultiCodegen(
    """
      @Qualifier annotation class MyQualifier<T>
      @Provide val qualifiedFoo: @MyQualifier<String> Foo = Foo()
    """,
    """
      fun invoke() = inject<@MyQualifier<String> Foo>() 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testQualifierWithGenericTypeArguments() = singleAndMultiCodegen(
    """
      @Qualifier annotation class MyQualifier<T>
      @Provide fun <T> qualifiedFoo(): @MyQualifier<T> Foo = Foo()
    """,
    """
      fun invoke() = inject<@MyQualifier<String> Foo>() 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testUiState() = singleAndMultiCodegen(
    """
      @Qualifier annotation class UiState

      @Provide fun <T> uiState(instance: @UiState T): T = instance

      @Provide val foo: @UiState Foo = Foo()
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testSubstitutesQualifierTypeParameters() = singleAndMultiCodegen(
    """
      @Provide fun foo(): @Eager<AppScope> Foo = Foo()
  
      typealias ChildScope = Scope
  
      @Provide val childScopeModule = ChildScopeModule0<AppScope, ChildScope>()
  
      @InstallElement<ChildScope>
      @Provide
      class MyElement(val foo: Foo)
    """,
    """
      @Providers("com.ivianuu.injekt.common.*", "com.ivianuu.injekt.scope.*")
      fun invoke() = inject<AppScope>()
    """
  ) {
    compilationShouldBeOk()
    irShouldNotContain("scopedImpl<Foo, Foo, U>(")
  }
}

