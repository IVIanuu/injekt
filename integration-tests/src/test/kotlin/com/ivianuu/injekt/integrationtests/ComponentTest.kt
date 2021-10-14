package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.singleAndMultiCodegen
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldBeTypeOf
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
      fun invoke() = inject<(@Provide Foo) -> MyComponent>()
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testScoped() = singleAndMultiCodegen(
    """
      @Component interface ScopeComponent {
        val foo: Foo
      } 

      @Provide val foo: @Scoped<ScopeComponent> Foo get() = Foo() 
    """,
    """
      val component = inject<ScopeComponent>()
      fun invoke() = component.foo
    """
  ) {
    invokeSingleFile() shouldBeSameInstanceAs invokeSingleFile()
  }

  @Test fun testCannotResolveScopedInjectableWithoutEnclosingComponent() = singleAndMultiCodegen(
    """
      interface ScopeComponent
      @Provide val foo: @Scoped<ScopeComponent> Foo = Foo() 
    """,
    """
      fun invoke() = inject<Foo>()
    """
  ) {
    compilationShouldHaveFailed("no enclosing component matches com.ivianuu.injekt.integrationtests.ScopeComponent")
  }

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
}