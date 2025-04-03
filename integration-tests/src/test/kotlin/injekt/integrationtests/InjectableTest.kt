/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalCompilerApi::class, ExperimentalCompilerApi::class)

package injekt.integrationtests

import io.kotest.matchers.collections.*
import io.kotest.matchers.types.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.junit.*

class InjectableTest {
  @Test fun testTopLevelInjectableFunction() = singleAndMultiCodegen(
    """
      @Provide fun foo() = Foo()
    """,
    """
      fun invoke() = create<Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testMemberInjectableFunction() = singleAndMultiCodegen(
    """
      @Provide class Baz {
        @Provide fun foo() = Foo()
      }
    """,
    """
      fun invoke() = create<Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testLocalInjectableFunction() = codegen(
    """
      fun invoke(): Foo {
        @Provide fun foo() = Foo()
        return create<Foo>()
      }
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableFunctionWithExtensionReceiver() = singleAndMultiCodegen(
    """
      @Provide fun Foo.bar() = Bar(this)
    """,
    """
      fun invoke() = with(Foo()) { create<Bar>() }.foo
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableFunctionWithContextParameter() = singleAndMultiCodegen(
    """
      context(foo: Foo) @Provide fun bar() = Bar(foo)
    """,
    """
      fun invoke() = with(Foo()) { create<Bar>() }.foo
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableFunctionWithMultipleAnonymousContextParameters() = singleAndMultiCodegen(
    """
      context(_: Foo, _: Bar) @Provide fun baz() = Baz(create(), create())
    """,
    """
      fun invoke() = with(Foo()) { 
        with(Bar(Foo())) { create<Baz>() }       
      }.foo
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testTopLevelInjectableProperty() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
    """,
    """
      fun invoke() = create<Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testMemberInjectableProperty() = singleAndMultiCodegen(
    """
      @Provide class Baz {
        @Provide val foo = Foo()
      }
    """,
    """
      fun invoke() = create<Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectablePropertyWithExtensionReceiver() = singleAndMultiCodegen(
    """
      @Provide val Foo.bar get() = Bar(this)
    """,
    """
      fun invoke() = with(Foo()) { create<Bar>() }.foo
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectablePropertyWithContextParameter() = singleAndMultiCodegen(
    """
      context(foo: Foo) @Provide val bar get() = Bar(foo)
    """,
    """
      fun invoke() = with(Foo()) { create<Bar>() }.foo
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testTopLevelInjectableClass() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide class Baz(val foo: Foo)
    """,
    """
      fun invoke() = create<Baz>().foo
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testNestedInjectableClass() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      class Qux {
        @Provide class Baz(val foo: Foo)
      }
    """,
    """
      fun invoke() = create<Qux.Baz>().foo
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInnerInjectableClass() = codegen(
    """
      @Provide class MyOuterClass {
        @Provide inner class MyInnerClass
      }
    """,
    """
      fun invoke() = create<MyOuterClass.MyInnerClass>()
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testLocalInjectableClass() = codegen(
    """
      fun invoke(): Foo {
        @Provide val foo = Foo()
        @Provide class Baz(val foo: Foo)
        return create<Baz>().foo
      }
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableAnnotationClass() = codegen(
    """
      @Provide annotation class MyAnnotation
    """,
    """
      fun invoke() = create<MyAnnotation>()
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testInjectablePrimaryConstructor() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      class Baz @Provide constructor(val foo: Foo)
    """,
    """
      fun invoke() = create<Baz>().foo
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableSecondaryConstructor() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      class Baz(val foo: Any) {
        @Provide constructor(foo: Foo) : this(foo as Any)
      }
    """,
    """
      fun invoke() = create<Baz>().foo
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testMultipleInjectableConstructors() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)
      class Baz(val foo: Any) {
        @Provide constructor(foo: Foo) : this(foo as Any)
        @Provide constructor(bar: Bar) : this(bar.foo as Any)
      }
    """,
    """
      fun invoke() = create<List<Baz>>().map { it.foo }
    """
  ) {
    invokeSingleFile<List<Foo>>()
      .shouldNotBeEmpty()
      .forEach { it.shouldBeTypeOf<Foo>() }
  }

  @Test fun testInjectableObject() = singleAndMultiCodegen(
    """
      @Provide object Baz {
        val foo = Foo()
      }
    """,
    """
      fun invoke() = create<Baz>().foo 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableAnonymousObject() = codegen(
    """
      interface A
      interface B
      fun invoke() {
        @Provide val instance = object : A, B  {
        }
        create<A>()
        create<B>()
      }
    """
  )

  @Test fun testInjectableValueParameter() = codegen(
    """
      fun invoke(@Provide foo: Foo) = create<Foo>()
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testInjectableFunctionExtensionReceiver() = singleAndMultiCodegen(
    """
      fun Foo.bar() = Bar(create())
    """,
    """
      fun invoke() = with(Foo()) { bar().foo }
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableFunctionContextParameter() = singleAndMultiCodegen(
    """
      context(_: Foo) fun bar() = Bar(create())
    """,
    """
      fun invoke() = with(Foo()) { bar().foo }
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableFunctionMultipleAnonymousContextParameters() = singleAndMultiCodegen(
    """
      context(_: Foo, _: Bar) fun baz(unit: Unit) = Baz(create(), create())
    """,
    """
      fun invoke() = with(Foo()) { 
        with(Bar(Foo())) { baz(Unit).foo } 
      }
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectablePropertyExtensionReceiver() = singleAndMultiCodegen(
    """
      val Foo.bar get() = Bar(create())
    """,
    """
      fun invoke() = with(Foo()) { bar.foo }
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectablePropertyContextParameter() = singleAndMultiCodegen(
    """
      context(_: Foo) val bar get() = Bar(create())
    """,
    """
      fun invoke() = with(Foo()) { bar.foo }
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableLocalVariable() = codegen(
    """
      fun invoke(foo: Foo): Foo {
        @Provide val providedFoo = foo
        return create()
      }
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testInjectableDelegatedLocalVariable() = codegen(
    """
      fun invoke(foo: Foo): Foo {
        @Provide val providedFoo by lazy { foo }
        return create()
      }
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testInjectableInLocalClass() = codegen(
    """
      fun invoke(): Foo {
        class Baz {
          @Provide fun foo() = Foo()
          
          val foo = create<Foo>()
        }

        return Baz().foo
      }
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableInAnonymousObject() = codegen(
    """
      fun invoke(): Foo {
        val baz = object : Any() {
          @Provide fun foo() = Foo()
          
          val foo = create<Foo>()
        }

        return baz.foo
      }
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableInAnonymousObject2() = codegen(
    """
      class MyClass(@property:Provide val foo: Foo = inject) {
        fun foo(): Foo {
          val someObject = object : Any() {
            fun foo() = create<Foo>()
          }

          return someObject.foo()
        }
      }

      fun invoke(): Foo = MyClass(Foo()).foo()
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableLambdaExtensionReceiver() = singleAndMultiCodegen("""
      fun lambdaOf(block: Foo.() -> Foo) = block
    """,
    """
      fun invoke() = lambdaOf { create<Foo>() }(Foo())
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableLambdaContextParameter() = singleAndMultiCodegen("""
      fun lambdaOf(block: context(Foo) () -> Foo) = block
    """,
    """
      fun invoke() = lambdaOf { create<Foo>() }(Foo())
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableLambdaParameterUseSite() = codegen("""
      fun invoke() = { foo: @Provide Foo -> create<Foo>() }(Foo())
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableLambdaParameterDeclarationSite() = singleAndMultiCodegen("""
      fun lambdaOf(block: (@Provide Foo) -> Foo) = block
    """,
    """
      fun invoke() = lambdaOf { create<Foo>() }(Foo())
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }
}
