package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.singleAndMultiCodegen
import org.junit.Test

class ComponentTest {
  @Test fun testComponentVal() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()      
      
      @Provide @Component interface FooComponent {
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
      
      @Provide @Component interface FooComponent {
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
      @Provide @Component interface MyComponent {
        var foo: Foo
      }
    """
  ) {
    compilationShouldHaveFailed("component cannot contain a abstract var property")
  }

  @Test fun testComponentFunctionWithParameters() = singleAndMultiCodegen(
    """
      @Provide fun bar(foo: Foo) = Bar(foo)

      @Provide @Component interface BarComponent {
        fun bar(foo: Foo): Bar
      } 
    """,
    """
      fun invoke() = inject<BarComponent>().bar(Foo())
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testComponentWithUnexistingRequestButDefaultImplementationIsNoError() = codegen(
    """
      @Provide @Component interface BarComponent {
        fun bar(foo: Foo): Bar = Bar(foo)
      }
  
      @Provide val foo = Foo()
  
      fun invoke() = inject<BarComponent>().bar(Foo())
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testComponentWithErrorRequestButDefaultImplementationIsNoError() = codegen(
    """
      @Provide @Component interface FooComponent {
        fun foo(): Foo = Foo()
      }
   
      fun invoke() = inject<FooComponent>().foo()
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testComponentWithTypeParameters() = codegen(
    """
      @Provide @Component interface ParameterizedComponent<T> {
        val value: T
      }
  
      @Provide val foo = Foo()
  
      fun invoke() = inject<ParameterizedComponent<Foo>>().value
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testComponentWithSuspendFunction() = codegen(
    """
      @Provide @Component interface FooComponent {
        suspend fun foo(): Foo
      }
  
      @Provide suspend fun foo() = Foo()
  
      fun invoke() = runBlocking { inject<FooComponent>().foo() }
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testComponentWithComposableProperty() = codegen(
    """
      @Provide @Component interface FooComponent {
        @Composable fun foo(): Foo
      }
  
      @Provide @Composable fun foo() = Foo()
  
      @Composable fun invoke() = inject<FooComponent>().foo()
    """
  )

  @Test fun testComponentIsCreatedOnTheFly() = codegen(
    """
      @Provide @Component interface MyComponent { 
        val foo: Foo
      }
  
      fun invoke() = inject<(@Provide Foo) -> MyComponent>()
    """
  ) {
    invokeSingleFile()
  }
}
