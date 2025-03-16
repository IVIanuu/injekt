package injekt.integrationtests

import io.kotest.matchers.types.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.junit.*

@OptIn(ExperimentalCompilerApi::class) class InjectTest {
  @Test fun testInjectFunctionValueParameter() = codegen(
    """
      fun invoke(foo: Foo = inject) = create<Foo>()
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testInjectConstructorValueParameter() = codegen(
    """
      class Baz(val foo: Foo = inject)
      fun invoke(foo: Foo = inject) = Baz().foo
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testDoesNotModifyPropertyAccessors() = codegen(
    """
      class MyClass {
        var foo: Foo? = null
        fun injectFoo(@Provide bar: Bar): Foo {
          this.foo = fooFromBar()
          return foo!!
        }

        fun fooFromBar(bar: Bar = inject) = bar.foo
      }
    """,
    """
      fun invoke() = MyClass().injectFoo(Bar(Foo()))
    """
  ) {
    invokeSingleFile().shouldBeInstanceOf<Foo>()
  }

  @Test fun testComposableInlineFunctionWithInjectParameters() = multiCodegen(
    """
      @Composable inline fun foo(foo: Foo = inject): Foo = foo
    """,
    """
      fun invoke(@Provide foo: Foo) = runComposing { foo() }
    """,
    config = { withCompose() }
  ) {
    invokeSingleFile(Foo())
  }
}
