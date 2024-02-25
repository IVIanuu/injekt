/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalCompilerApi::class, ExperimentalCompilerApi::class)

package com.ivianuu.injekt.integrationtests

import io.kotest.matchers.*
import io.kotest.matchers.types.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.junit.*

class InjectableDeclarationTest {
  @Test fun testInjectableFunction() = singleAndMultiCodegen(
    """
      @Provide fun foo(): Foo = Foo()
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableProperty() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectableClass() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide class Dep(val foo: Foo)
    """,
    """
      fun invoke() = inject<Dep>()
    """
  ) {
    invokeSingleFile<Any>().javaClass.name shouldBe "com.ivianuu.injekt.integrationtests.Dep"
  }

  @Test fun testInjectablePrimaryConstructor() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      class Dep @Provide constructor(val foo: Foo)
    """,
    """
      fun invoke() = inject<Dep>() 
    """
  ) {
    invokeSingleFile<Any>().javaClass.name shouldBe "com.ivianuu.injekt.integrationtests.Dep"
  }

  @Test fun testInjectableSecondaryConstructor() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      class Dep {
        @Provide constructor(foo: Foo)
      }
    """,
    """
      fun invoke() = inject<Dep>() 
    """
  ) {
    invokeSingleFile<Any>().javaClass.name shouldBe "com.ivianuu.injekt.integrationtests.Dep"
  }

  @Test fun testClassWithMultipleProvideConstructors() = singleAndMultiCodegen(
    """
      class Dep {
        @Provide constructor(foo: Foo)
        @Provide constructor(bar: Bar)
      }
    """,
    """
      fun invoke() = inject<(Foo) -> Dep>() to inject<(Bar) -> Dep>()
    """
  )

  @Test fun testNestedInjectableClass() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      class Outer {
        @Provide class Dep(val foo: Foo)
      }
    """,
    """
      fun invoke() = inject<Outer.Dep>() 
    """
  ) {
    invokeSingleFile<Any>().javaClass.name shouldBe "com.ivianuu.injekt.integrationtests.Outer\$Dep"
  }

  @Test fun testInjectableObject() = singleAndMultiCodegen(
    """
      @Provide object Dep
    """,
    """
      fun invoke() = inject<Dep>() 
    """
  ) {
    invokeSingleFile<Any>().javaClass.name shouldBe "com.ivianuu.injekt.integrationtests.Dep"
  }

  @Test fun testInjectableFunctionExtensionReceiver() = singleAndMultiCodegen(
    """
      fun @receiver:Provide Foo.bar() = Bar(inject())
    """,
    """
      fun invoke() = with(Foo()) { bar() }
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Bar>()
  }

  @Test fun testInjectablePropertyExtensionReceiver() = singleAndMultiCodegen(
    """
      val @receiver:Provide Foo.bar get() = Bar(inject())
    """,
    """
      fun invoke() = with(Foo()) { bar }
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Bar>()
  }

  @Test fun testInjectableValueParameter() = codegen(
    """
      fun invoke(@Provide foo: Foo) = inject<Foo>()
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testInjectValueParameter() = codegen(
    """
      fun invoke(foo: Foo = inject) = inject<Foo>()
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
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

  @Test fun testInjectableLambdaParameterUseSite() = singleAndMultiCodegen(
    """
      inline fun <T, R> withProvidedInstance(value: T, block: (T) -> R) = block(value)
    """,
    """
      fun invoke(foo: Foo) = withProvidedInstance(foo) { foo: @Provide Foo -> inject<Foo>() }
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testInjectFunInterfaceDeclarationSite() = singleAndMultiCodegen(
    """
      fun interface Lambda<T, R> {
        fun actualInvoke(@Provide x: T): R
        operator fun invoke(x: T = inject) = actualInvoke(x)
      }

      fun <T, R> withProvidedInstance(value: T, block: Lambda<T, R>) = block(value)
    """,
    """
      fun invoke(foo: Foo) = withProvidedInstance(foo, Lambda { inject<Foo>() })
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testInjectableLocalClass() = codegen(
    """
      fun invoke(@Provide _foo: Foo): Foo {
        @Provide class FooProvider(val foo: Foo)
        return inject<FooProvider>().foo
      }
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testInjectableLocalFunction() = codegen(
    """
      fun invoke(foo: Foo): Foo {
        @Provide fun foo() = foo
        return inject<Foo>()
      }
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testLocalObject() = codegen(
    """
      interface A
      interface B
      fun invoke() {
        @Provide val instance = object : A, B  {
        }
        inject<A>()
        inject<B>()
      }
    """
  )

  @Test fun testSuperClassPrimaryInjectableConstructorParameter() = codegen(
    """
      abstract class MySuperClass(@property:Provide val foo: Foo)
    """,
    """
      @Provide object MySubClass : MySuperClass(Foo())
      fun invoke() = inject<Foo>()
    """
  )

  @Test fun testInjectableInLocalClass() = codegen(
    """
      fun invoke() {
        class MyClass {
          @Provide fun foo() = Foo()
          
          override fun equals(other: Any?): Boolean {
            inject<Foo>()
            return super.equals(other)
          }
        }
      }
    """
  )

  @Test fun testInjectableInAnonymousObject() = codegen(
    """
      fun invoke() {
        object : Any() {
          @Provide fun foo() = Foo()
          
          override fun equals(other: Any?): Boolean {
            inject<Foo>()
            return super.equals(other)
          }
        }
      }
    """
  )
}
