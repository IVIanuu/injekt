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
import com.ivianuu.injekt.test.TestComponentObserver
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.irShouldContain
import com.ivianuu.injekt.test.shouldContainMessage
import com.ivianuu.injekt.test.shouldNotContainMessage
import com.ivianuu.injekt.test.singleAndMultiCodegen
import com.ivianuu.injekt.test.source
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldBeTypeOf
import org.jetbrains.kotlin.name.FqName
import org.junit.Test

class ComponentTest {
  @Test fun testComponentVal() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()      
      
      @Component interface FooComponent {
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
      
      @Component interface FooComponent {
        fun foo(): Foo
      }
    """,
    """
      fun invoke() = inject<FooComponent>().foo()
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testComponentWithVar() = codegen(
    """
      @Component interface MyComponent {
        var foo: Foo
      }
    """
  ) {
    compilationShouldHaveFailed("component cannot contain a abstract var property")
  }

  @Test fun testComponentFunctionWithParameters() = singleAndMultiCodegen(
    """
      @Provide fun bar(foo: Foo) = Bar(foo)

      @Component interface BarComponent {
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

      @Component interface BarComponent {
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

      @Component interface BarComponent {
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
      @Component interface BarComponent {
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
      @Component interface FooComponent {
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
      @Component interface ParameterizedComponent<T> {
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
      @Component interface FooComponent {
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

  @Test fun testComponentWithComposableProperty() = singleAndMultiCodegen(
    """
      @Component interface FooComponent {
        @Composable fun foo(): Foo
      }
  
      @Provide @Composable fun foo() = Foo()
    """,
    """
      @Composable fun invoke() = inject<FooComponent>().foo()
    """
  )

  @Test fun testComponentIsCreatedOnTheFly() = singleAndMultiCodegen(
    """
      @Component interface MyComponent { 
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
      @Component interface MyComponent<T> {
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
      @Component interface MyComponent

      @EntryPoint<MyComponent> interface MyEntryPoint {
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
      @Component interface MyComponent

      @EntryPoint<C> interface MyEntryPoint<C : @Component Any> {
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
      @Component interface MyComponent

      @EntryPoint<C> interface CoroutineScopeComponent<C : @Component Any> {
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
      @Component interface MyComponent
    """,
    """
      fun invoke() = inject<MyComponent>()
    """
  ) {
    invokeSingleFile<Disposable>().dispose()
  }

  @Test fun testComponentObserver() = singleAndMultiCodegen(
    """
      @Component interface MyComponent
    """,
    """
      fun invoke(@Inject observer: ComponentObserver<MyComponent>): () -> MyComponent = {
        inject<MyComponent>()
      }
    """
  ) {
    val observer = TestComponentObserver<Any>()
    val componentFactory = invokeSingleFile<() -> Disposable>(observer)
    observer.initCalls shouldBe 0
    observer.disposeCalls shouldBe 0
    val component = componentFactory()
    observer.initCalls shouldBe 1
    observer.disposeCalls shouldBe 0
    component.dispose()
    observer.initCalls shouldBe 1
    observer.disposeCalls shouldBe 1
  }

  @Test fun testComponentObserverWhoUsesScopedInjectables() = singleAndMultiCodegen(
    """
      @Component interface MyComponent
    """,
    """
      class MyObserver<C : @Component Any> @Provide @Scoped<C> constructor() : ComponentObserver<C>

      fun invoke(): MyComponent = inject<MyComponent>()
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testCanInjectComponent() = singleAndMultiCodegen(
    """
      @Component interface MyComponent {
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
      @Component interface MyComponent {
        val dep: Dep
      }

      @EntryPoint<MyComponent> interface MyEntryPoint

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

      @Component interface MyComponent : BaseComponent1, BaseComponent2

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
      @Component interface MyComponent {
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
      @Component @Tag1 interface MyComponent
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
      @Component abstract class FooComponent {
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
      @Component abstract class FooComponent(private val _foo: Foo) {
        abstract val foo: Foo
        @Provide protected fun foo() = _foo
      }
    """,
    """
      fun invoke(@Provide foo: Foo) = inject<(Foo) -> FooComponent>()(foo).foo
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testComponentCannotUseItsOwnInjectables() = singleAndMultiCodegen(
    """
      @Component interface FooComponent {
        @Provide val foo: Foo
      }
    """,
    """
      fun invoke() = inject<FooComponent>()
    """
  ) {
    compilationShouldHaveFailed(
      "no injectable found of type com.ivianuu.injekt.test.Foo"
    )
  }

  @Test fun testComponentTypeScope() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Component interface MyComponent
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

  @Test fun testNonAbstractComponent() = codegen(
    """ 
      @Component class FooComponent
    """
  ) {
    compilationShouldHaveFailed("component must be either a abstract class or a interface")
  }

  @Test fun testNonAbstractEntryPoint() = codegen(
    """ 
      @EntryPoint<Any> class FooComponent
    """
  ) {
    compilationShouldHaveFailed("entry point must be a interface")
  }
}
