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

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.singleAndMultiCodegen
import io.kotest.matchers.types.shouldBeTypeOf
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import org.junit.Test

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

  @Test fun testTaggedFunction() = singleAndMultiCodegen(
    """ 
      @Provide @Tag1 fun foo() = Foo()
    """,
    """
      fun invoke() = inject<@Tag Foo>()
    """
  )

  @Test fun testTagWithValueParameters() = codegen(
    """ 
      @Tag annotation class MyTag(val value: String)
    """
  ) {
    compilationShouldHaveFailed("tag cannot have value parameters")
  }

  @Test fun testTagWithTypeParameters() = singleAndMultiCodegen(
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

  @Test fun testTagTypeAliasPattern() = singleAndMultiCodegen(
    """
      @Tag annotation class TaggedFooTag
      typealias TaggedFoo = @TaggedFooTag Foo
      @Provide val taggedFoo: TaggedFoo = Foo()
    """,
    """
      fun invoke() = inject<TaggedFoo>()
    """
  )

  @Test fun testGenericTagTypeAliasPattern() = singleAndMultiCodegen(
    """
      @Component interface MyComponent {
        val scope: ComponentScope<MyComponent>        
      }

      typealias ComponentScope<C> = @ComponentScopeTag<C> String

      @Tag annotation class ComponentScopeTag<C : @Component Any> {
        companion object {
          @Provide @Scoped<C> fun <C : @Component Any> scope(): ComponentScope<C> = ""
        }
      }
    """,
    """
      fun invoke() = inject<MyComponent>()
    """
  )
}
