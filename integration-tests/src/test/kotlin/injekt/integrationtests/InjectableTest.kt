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
      fun @receiver:Provide Foo.bar() = Bar(inject())
    """,
    """
      fun invoke() = with(Foo()) { bar().foo }
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectablePropertyExtensionReceiver() = singleAndMultiCodegen(
    """
      val @receiver:Provide Foo.bar get() = Bar(inject())
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
        return inject()
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
        return inject()
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

  @Test fun testInjectableLambdaParameterUseSite() = codegen("""
      fun invoke() = { foo: @Provide Foo -> create<Foo>() }(Foo())
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableLambdaParameterDeclarationSite() = singleAndMultiCodegen("""
      fun lambdaOf(block: (@Provide Foo).() -> Foo) = block
    """,
    """
      fun invoke() = lambdaOf { create<Foo>() }(Foo())
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableLambdaReceiver() = singleAndMultiCodegen("""
      fun lambdaOf(block: (@Provide Foo).() -> Foo) = block
    """,
    """
      fun invoke() = lambdaOf { create<Foo>() }(Foo())
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableFunInterfaceParameterUseSite() = codegen(
    """
      fun interface Lambda<T> { fun invoke(t: T): T }
      fun invoke() = Lambda<Foo> { foo: @Provide Foo -> create<Foo>() }.invoke(Foo())
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableFunInterfaceParameterDeclarationSite() = singleAndMultiCodegen("""
      fun interface Lambda<T> { fun invoke(@Provide t: T): T }
    """,
    """
      fun invoke() = Lambda<Foo> { create<Foo>() }.invoke(Foo())
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableFunInterfaceReceiver() = singleAndMultiCodegen("""
      fun interface Lambda<T> { fun @receiver:Provide T.invoke(): T }
    """,
    """
      fun invoke() = with(Lambda<Foo> { create<Foo>() }) { with(Foo()) { invoke() } }
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }
}
