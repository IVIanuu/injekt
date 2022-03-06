/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.*
import io.kotest.matchers.*
import io.kotest.matchers.nulls.*
import io.kotest.matchers.types.*
import org.jetbrains.kotlin.name.*
import org.junit.*

class InjectableDeclarationTest {
  @Test fun testProvideFunction() = singleAndMultiCodegen(
    """
      @Provide fun foo() = Foo()
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testProvideProperty() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testProvideClass() = singleAndMultiCodegen(
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

  @Test fun testProvideClassPrimaryConstructor() = singleAndMultiCodegen(
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

  @Test fun testProvideClassSecondaryConstructor() = singleAndMultiCodegen(
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
      @Provide val foo = Foo()
    """,
    """
      fun invoke() = inject<Dep>() 
    """
  )

  @Test fun testNestedProvideClass() = singleAndMultiCodegen(
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

  @Test fun testProvideObject() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide object Dep {
        init {
          inject<Foo>()
        }
      }
    """,
    """
      fun invoke() = inject<Dep>() 
    """
  ) {
    invokeSingleFile<Any>().javaClass.name shouldBe "com.ivianuu.injekt.integrationtests.Dep"
  }

  @Test fun testProvideCompanionObject() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      class Dep {
        @Provide companion object {
          init {
            inject<Foo>()
          }
        }
      }
    """,
    """
      fun invoke() = inject<Dep.Companion>() 
    """
  ) {
    invokeSingleFile<Any>().javaClass.name shouldBe "com.ivianuu.injekt.integrationtests.Dep\$Companion"
  }

  @Test fun testProvideExtensionFunction() = singleAndMultiCodegen(
    """
      @Provide fun Foo.bar() = Bar(this)
    """,
    """
      fun invoke(@Inject foo: Foo) = inject<Bar>() 
    """
  ) {
    invokeSingleFile(Foo())
      .shouldBeTypeOf<Bar>()
  }

  @Test fun testProvideExtensionProperty() = singleAndMultiCodegen(
    """
      @Provide val Foo.bar get() = Bar(this)
    """,
    """
      fun invoke(@Inject foo: Foo) = inject<Bar>() 
    """
  ) {
    invokeSingleFile(Foo())
      .shouldBeTypeOf<Bar>()
  }

  @Test fun testProvideDispatchReceiver() = codegen(
    """
      class MyClass {
        fun receiver() = inject<MyClass>()
      }

      fun invoke(): Pair<Any, Any> {
        val instance = MyClass()
        return instance to instance.receiver()
      }
    """
  ) {
    val (a, b) = invokeSingleFile<Pair<Any, Any>>()
    a shouldBeSameInstanceAs b
  }

  @Test fun testProvideExtensionReceiver() = codegen(
    """
      fun Foo.foo() = inject<Foo>()

      fun invoke(foo: Foo) = foo.foo()
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testProvideValueParameter() = codegen(
    """
      fun invoke(@Provide foo: Foo) = inject<Foo>()
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testInjectValueParameter() = codegen(
    """
      fun invoke(@Inject foo: Foo) = inject<Foo>()
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testMultipleInjectValueParameter() = codegen(
    """
      fun invoke(@Inject foo: Foo, @Inject bar: Bar) = inject<Foo>() to inject<Bar>()
    """
  ) {
    val foo = Foo()
    val bar = Bar(foo)
    val (a, b) = invokeSingleFile<Pair<Foo, Bar>>(foo, bar)
    foo shouldBeSameInstanceAs a
    bar shouldBeSameInstanceAs b
  }

  @Test fun testProvideLocalVariable() = codegen(
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

  @Test fun testProvideDelegatedLocalVariable() = codegen(
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

  @Test fun testGenericInjectable() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide fun <T> injectableList(value: T): List<T> = listOf(value)
    """,
    """
      fun invoke() = inject<List<Foo>>() 
    """
  ) {
    val (foo) = invokeSingleFile<List<Any>>()
    foo.shouldBeTypeOf<Foo>()
  }

  @Test fun testInjectLambdaParameterDeclarationSite() = singleAndMultiCodegen(
    """
      inline fun <T, R> withProvidedInstance(value: T, block: (@Inject T) -> R) = block(value)
    """,
    """
      fun invoke(foo: Foo) = withProvidedInstance(foo) { inject<Foo>() }
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testMultipleInjectLambdaParameterDeclarationSite() = singleAndMultiCodegen(
    """
      inline fun <T, S, R> withProvidedInstances(@Provide t: T, @Provide s: S, block: (@Inject T, @Inject S) -> R) = block()
    """,
    """
      fun invoke(foo: Foo, bar: Bar) = withProvidedInstances(foo, bar) { _, _ -> inject<Foo>() to inject<Bar>() }
    """
  ) {
    val foo = Foo()
    val bar = Bar(foo)
    val (a, b) = invokeSingleFile<Pair<Foo, Bar>>(foo, bar)
    a shouldBeSameInstanceAs foo
    b shouldBeSameInstanceAs bar
  }

  @Test fun testCanLeaveOutFunctionInjectParameters() = singleAndMultiCodegen(
    """
      fun usesFoo(@Inject foo: Foo) {
      }
    """,
    """
      @Provide val foo = Foo()
      fun invoke() {
        usesFoo()
      }
    """
  )

  @Test fun testCanLeaveOutConstructorInjectParameters() = singleAndMultiCodegen(
    """
      class FooHolder(@Inject foo: Foo)
    """,
    """
      @Provide val foo = Foo()
      fun invoke() {
        FooHolder()
      }
    """
  )

  @Test fun testCanLeaveOutSuperConstructorInjectParameters() = singleAndMultiCodegen(
    """
      abstract class AbstractFooHolder(@Inject foo: Foo)
    """,
    """
      @Provide val foo = Foo()
      class FooHolderImpl : AbstractFooHolder()
    """
  )

  @Test fun testCanLeaveOutInjectLambdaParameters() = singleAndMultiCodegen(
    """
      val lambda: (@Inject Foo) -> Foo = { inject<Foo>() }
    """,
    """
      fun invoke(@Inject foo: Foo) = lambda()
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testCanLeaveOutInjectExtensionLambdaParameters() = singleAndMultiCodegen(
    """
      val lambda: Unit.(@Inject Foo) -> Foo = { inject<Foo>() }
    """,
    """
      fun invoke(@Inject foo: Foo) = lambda(Unit)
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testCanLeaveOutInjectSuspendLambdaParameters() = singleAndMultiCodegen(
    """
      val lambda: suspend (@Inject Foo) -> Foo = { inject<Foo>() }
    """,
    """
      fun invoke(@Inject foo: Foo) = runBlocking { lambda() }
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testCanLeaveOutInjectComposableLambdaParameters() = singleAndMultiCodegen(
    """
      val lambda: @Composable (@Inject Foo) -> Foo = { inject<Foo>() }
    """,
    """
      fun invoke(@Inject foo: Foo) = runComposing { lambda() }
    """,
    config = { withCompose() }
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testProvideLambdaParameterUseSite() = singleAndMultiCodegen(
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

  @Test fun testProvideLocalFunction() = codegen(
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

  @Test fun testProvideLocalClass() = codegen(
    """
      fun invoke(_foo: Foo): Foo {
        @Provide class FooProvider(__foo: Foo = _foo) {
          val foo = __foo
        }
        return inject<FooProvider>().foo
      }
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testProvideAnonymousObject() = codegen(
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

  @Test fun testProvideSuspendFunction() = singleAndMultiCodegen(
    """
      @Provide suspend fun foo() = Foo()
    """,
    """
      fun invoke() = runBlocking { inject<Foo>() } 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testProvideComposableFunction() = singleAndMultiCodegen(
    """
      @Provide @Composable fun foo() = Foo()
    """,
    """
      fun invoke() = runComposing { inject<Foo>()  }
    """,
    config = { withCompose() }
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testSuperClassPrimaryProvideConstructorParameter() = singleAndMultiCodegen(
    """
      abstract class MySuperClass(@Provide val foo: Foo)
    """,
    """
      @Provide object MySubClass : MySuperClass(Foo())
      fun invoke() = inject<Foo>()
    """
  )

  @Test fun testProvideFunctionInLocalClass() = codegen(
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

  @Test fun testProvidePropertyInLocalClass() = codegen(
    """
      fun invoke() {
        class MyClass {
          @Provide val foo = Foo()
          
          override fun equals(other: Any?): Boolean {
            inject<Foo>()
            return super.equals(other)
          }
        }
      }
    """
  )

  @Test fun testProvideFunctionInAnonymousObject() = codegen(
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

  @Test fun testProvidePropertyInAnonymousObject() = codegen(
    """
      fun invoke() {
        object : Any() {
          @Provide private val foo = Foo()
          
          override fun equals(other: Any?): Boolean {
            inject<Foo>()
            return super.equals(other)
          }
        }
      }
    """
  )
}
