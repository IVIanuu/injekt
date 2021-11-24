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

import com.ivianuu.injekt.common.Disposable
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokableSource
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.irShouldContain
import com.ivianuu.injekt.test.shouldContainMessage
import com.ivianuu.injekt.test.shouldNotContainMessage
import com.ivianuu.injekt.test.singleAndMultiCodegen
import com.ivianuu.injekt.test.source
import com.ivianuu.injekt.test.withCompose
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldBeTypeOf
import org.jetbrains.kotlin.name.FqName
import org.junit.Test

class ComponentTest {
  @Test fun testComponentVal() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()      
      
      @Provide interface FooComponent : Component {
        val foo: Foo
      }
    """,
    """
      fun invoke() = inject<FooComponent>().foo
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testComponentFunction() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()      
      
      @Provide interface FooComponent : Component {
        fun foo(): Foo
      }
    """,
    """
      fun invoke() = inject<FooComponent>().foo()
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testComponentFunctionWithParameters() = singleAndMultiCodegen(
    """
      @Provide fun bar(foo: Foo) = Bar(foo)

      @Provide interface BarComponent : Component {
        fun bar(foo: Foo): Bar
      } 
    """,
    """
      fun invoke() = inject<BarComponent>().bar(Foo())
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testComponentFunctionWithExtensionReceiver() = singleAndMultiCodegen(
    """
      @Provide fun bar(foo: Foo) = Bar(foo)

      @Provide interface BarComponent : Component {
        fun Foo.bar(): Bar
      } 
    """,
    """
      fun invoke() = with(inject<BarComponent>()) {
        with(Foo()) {
          bar()
        }
      }
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testComponentPropertyWithExtensionReceiver() = singleAndMultiCodegen(
    """
      @Provide fun bar(foo: Foo) = Bar(foo)

      @Provide interface BarComponent : Component {
        val Foo.bar: Bar
      } 
    """,
    """
      fun invoke() = with(inject<BarComponent>()) {
        with(Foo()) {
          bar
        }
      }
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testComponentWithUnexistingRequestButDefaultImplementationIsNoError() = singleAndMultiCodegen(
    """
      @Provide interface BarComponent : Component {
        fun bar(foo: Foo): Bar = Bar(foo)
      }
  
      @Provide val foo = Foo()
    """,
    """
      fun invoke() = inject<BarComponent>().bar(Foo())
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testComponentWithErrorRequestButDefaultImplementationIsNoError() = singleAndMultiCodegen(
    """
      @Provide interface FooComponent : Component {
        fun foo(): Foo = Foo()
      }
    """,
    """
      fun invoke() = inject<FooComponent>().foo()
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testComponentWithTypeParameters() = singleAndMultiCodegen(
    """
      @Provide interface ParameterizedComponent<T> : Component {
        val value: T
      }
  
      @Provide val foo = Foo()
    """,
    """
      fun invoke() = inject<ParameterizedComponent<Foo>>().value
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testComponentWithSuspendFunction() = singleAndMultiCodegen(
    """
      @Provide interface FooComponent : Component {
        suspend fun foo(): Foo
      }
  
      @Provide suspend fun foo() = Foo()
    """,
    """
      fun invoke() = runBlocking { inject<FooComponent>().foo() }
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testComponentWithComposableFunction() = singleAndMultiCodegen(
    """
      @Provide interface FooComponent : Component {
        @Composable fun foo(): Foo
      }
  
      @Provide @Composable fun foo() = Foo()
    """,
    """
      @Composable fun invoke() = inject<FooComponent>().foo()
    """,
    config = { withCompose() }
  )

  @Test fun testComponentWithComposableProperty() = singleAndMultiCodegen(
    """
      @Provide interface FooComponent : Component {
        @Composable fun foo(): Foo
      }
  
      @Provide @Composable fun foo() = Foo()
    """,
    """
      @Composable fun invoke() = inject<FooComponent>().foo()
    """,
    config = { withCompose() }
  )

  @Test fun testComponentIsCreatedOnTheFly() = singleAndMultiCodegen(
    """
      @Provide interface MyComponent : Component { 
        val foo: Foo
      }
    """,
    """
      fun invoke() = inject<(Foo) -> MyComponent>()
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testGenericComponent() = singleAndMultiCodegen(
    """
      @Provide interface MyComponent<T> : Component {
        val value: T
      }
      @Provide val foo = Foo()
    """,
    """
      fun invoke() = inject<MyComponent<Foo>>()
    """
  )

  @Test fun testEntryPoint() = singleAndMultiCodegen(
    """
      @Provide interface MyComponent : Component

      @Provide interface MyEntryPoint : EntryPoint<MyComponent> {
        val foo: Foo
      } 

      @Provide val foo = Foo() 
    """,
    """
      fun invoke() = (inject<MyComponent>() as MyEntryPoint).foo
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testGenericEntryPoint() = singleAndMultiCodegen(
    """
      @Provide interface MyComponent : Component

      @Provide interface MyEntryPoint<C : Component> : EntryPoint<C> {
        val foo: Foo
      }

      @Provide val foo = Foo() 
    """,
    """
      fun invoke() = (inject<MyComponent>() as MyEntryPoint<MyComponent>).foo
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testGenericScopeCallable() = singleAndMultiCodegen(
    """
      @Provide interface MyComponent : Component

      @Provide interface CoroutineScopeComponent<C : Component> : EntryPoint<C> {
        val coroutineScope: com.ivianuu.injekt.coroutines.ComponentScope<C>
      }
    """,
    """
      @Providers("com.ivianuu.injekt.coroutines.*")
      fun invoke() = inject<MyComponent>()
    """
  )

  @Test fun testComponentIsDisposable() = singleAndMultiCodegen(
    """
      @Provide interface MyComponent : Component
    """,
    """
      fun invoke() = inject<MyComponent>()
    """
  ) {
    invokeSingleFile<Disposable>().dispose()
  }

  @Test fun testCanInjectComponent() = singleAndMultiCodegen(
    """
      @Provide interface MyComponent : Component {
        val dep: Dep
      }

      @Provide class Dep(val component: MyComponent)
    """,
    """
      fun invoke() = inject<MyComponent>()
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testCanInjectEntryPoint() = singleAndMultiCodegen(
    """
      @Provide interface MyComponent : Component {
        val dep: Dep
      }

      @Provide interface MyEntryPoint : EntryPoint<MyComponent>

      @Provide class Dep(val entryPoint: MyEntryPoint)
    """,
    """
      fun invoke() = inject<MyComponent>()
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testComponentDoesIncludeDuplicates() = singleAndMultiCodegen(
    """
      interface BaseComponent1 {
        val foo: Foo
      }
      interface BaseComponent2 {
        val foo: Foo
      }

      @Provide interface MyComponent : Component, BaseComponent1, BaseComponent2

      @Provide val foo = Foo()
    """,
    """
      fun invoke() = inject<MyComponent>()
    """
  ) {
    irShouldContain(1, "override val foo")
  }

  @Test fun testComponentDisposeWithUninitializedScopedValue() = singleAndMultiCodegen(
    """
      @Provide interface MyComponent : Component {
        val foo: Foo
      }

      @Provide @Scoped<MyComponent> val foo = Foo()
    """,
    """
      fun invoke() = inject<MyComponent>().dispose()
    """
  )

  @Test fun testTaggedComponent() = singleAndMultiCodegen(
    """
      @Provide @Tag1 interface MyComponent : Component
    """,
    """
      fun invoke() {
        inject<@Tag1 MyComponent>()
        inject<MyComponent>()
      }
    """
  ) {
    shouldContainMessage(
      "no injectable found of type com.ivianuu.injekt.integrationtests.MyComponent"
    )
    shouldNotContainMessage(
      "no injectable found of type com.ivianuu.injekt.test.Tag1<com.ivianuu.injekt.integrationtests.MyComponent>"
    )
  }

  @Test fun testAbstractComponentClass() = singleAndMultiCodegen(
    """ 
      @Provide abstract class FooComponent : Component {
        abstract val foo: Foo
        @Provide protected fun foo() = Foo()
      }
    """,
    """
      fun invoke() = inject<FooComponent>().foo
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testAbstractComponentClassWithConstructorDependencies() = singleAndMultiCodegen(
    """ 
      @Provide abstract class FooComponent(protected val _foo: Foo) : Component {
        abstract val foo: Foo
      }
    """,
    """
      fun invoke(foo: Foo) = inject<(Foo) -> FooComponent>()(foo).foo
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testComponentTypeScope() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide interface MyComponent : Component
          """,
          packageFqName = FqName("component")
        )
      ),
      listOf(
        source(
          """
            fun invoke() = inject<component.MyComponent>()
          """
        )
      )
    )
  )

  @Test fun testComponentWithClashingEntryPoints() = singleAndMultiCodegen(
    """ 
      @Provide interface MyComponent : Component

      @Provide interface EntryPointA : EntryPoint<MyComponent> {
        fun a(): String
      }

      @Provide interface EntryPointB : EntryPoint<MyComponent> {
        fun a(): Int
      }
    """,
    """
      fun invoke() = inject<MyComponent>()
    """
  ) {
    compilationShouldHaveFailed("com.ivianuu.injekt.integrationtests.MyComponent has clashing super types com.ivianuu.injekt.integrationtests.EntryPointA and com.ivianuu.injekt.integrationtests.EntryPointB")
  }

  @Test fun testSealedAbstractInjectable() = codegen(
    """ 
      @Provide sealed interface MyComponent : Component
    """
  ) {
    compilationShouldHaveFailed("abstract injectable cannot be sealed")
  }

  @Test fun testIncludesTypeScopeEntryPoints() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide interface MyComponent : Component

            @Provide interface ComponentPackageEntryPoint : EntryPoint<MyComponent>
          """,
          packageFqName = FqName("component")
        )
      ),
      listOf(
        source(
          """
            @Provide interface EntryPointPackageEntryPoint<C : Component> : EntryPoint<C>
          """,
          packageFqName = FqName("com.ivianuu.injekt.common")
        )
      ),
      listOf(
        invokableSource(
          """
            fun invoke() = (inject<component.MyComponent>() as? component.ComponentPackageEntryPoint) to
              (inject<component.MyComponent>() as? EntryPointPackageEntryPoint<*>)
          """
        )
      )
    )
  ) {
    val (a, b) = invokeSingleFile<Pair<Any?, Any?>>()
    a.shouldNotBeNull()
    b.shouldNotBeNull()
  }
}
