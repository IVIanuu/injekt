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

class TagTest {
  @Test fun testDistinctTag() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide val taggedFoo: @Tag1 Foo = Foo()
    """,
    """
      fun invoke(): Pair<Foo, Foo> {
        return inject<Foo>() to inject<@Tag1 Foo>()
      } 
    """
  ) {
    val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
    foo1 shouldNotBeSameInstanceAs foo2
  }

  @Test fun testTypeParameterWithTagUpperBound() = singleAndMultiCodegen(
    """
      @Provide class Dep<T>(val value: @Tag1 T)
            
      @Provide fun tagged(): @Tag1 String = ""
    """,
    """
      fun invoke() = inject<Dep<String>>() 
    """
  )

  @Test fun testTaggedClass() = singleAndMultiCodegen(
    """ 
      @Provide @Tag1 class Dep
    """,
    """
      fun invoke() = inject<@Tag1 Dep>()
    """
  )

  @Test fun testTaggedPrimaryConstructor() = singleAndMultiCodegen(
    """ 
      class Dep @Provide @Tag1 constructor()
    """,
    """
      fun invoke() = inject<@Tag1 Dep>()
    """
  )

  @Test fun testTaggedSecondaryConstructor() = singleAndMultiCodegen(
    """ 
      class Dep {
        @Provide @Tag1 constructor()
      }
    """,
    """
      fun invoke() = inject<@Tag1 Dep>()
    """
  )

  @Test fun testTaggedObject() = singleAndMultiCodegen(
    """ 
      @Provide @Tag1 object Dep
    """,
    """
      fun invoke() = inject<@Tag1 Dep>()
    """
  )

  @Test fun testTaggedFunction() = codegen(
    """ 
      @Provide @Tag1 fun foo() = Foo()
    """
  ) {
    compilationShouldHaveFailed("only types, classes and class constructors can be annotated with a tag")
  }

  @Test fun testTagWithArguments() = codegen(
    """ 
      @Tag annotation class MyTag(val value: String)
    """
  ) {
    compilationShouldHaveFailed("tag cannot have value parameters")
  }

  @Test fun testTagWithTypeArguments() = singleAndMultiCodegen(
    """
      @Tag annotation class MyTag<T>
      @Provide val taggedFoo: @MyTag<String> Foo = Foo()
    """,
    """
      fun invoke() = inject<@MyTag<String> Foo>() 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testTagWithGenericTypeArguments() = singleAndMultiCodegen(
    """
      @Tag annotation class MyTag<T>
      @Provide fun <T> taggedFoo(): @MyTag<T> Foo = Foo()
    """,
    """
      fun invoke() = inject<@MyTag<String> Foo>() 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testUiState() = singleAndMultiCodegen(
    """
      @Tag annotation class UiState

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

  @Test fun testSubstitutesTagTypeParameters() = singleAndMultiCodegen(
    """
      @Provide fun foo(): @Eager<AppScope> Foo = Foo()
  
      typealias ChildScope = Scope
  
      @Provide val childScopeModule = ChildScopeModule0<AppScope, ChildScope>()
  
      @ScopeElement<ChildScope>
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

