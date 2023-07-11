/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Bar
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.singleAndMultiCodegen
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.Test

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
    """,
    """
      fun invoke() = inject<(Foo) -> Dep>() to inject<(Bar) -> Dep>()
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

  @Test fun testProvideInnerClass() = codegen(
    """
      class Outer(@Provide val _foo: Foo) {
        val foo = Inner().foo
        inner class Inner(@Inject val foo: Foo)
      }
      fun invoke(foo: Foo): Foo = Outer(foo).foo
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testProvideNestedClass() = codegen(
    """
      class Outer(@Provide val _foo: Foo) {
        val foo = Inner().foo
        class Inner(@Inject val foo: Foo)
      }
      fun invoke(foo: Foo): Foo = Outer(foo).foo
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testSuperClassPrimaryProvideConstructorParameter() = singleAndMultiCodegen(
    """
      abstract class MySuperClass(@property:Provide val foo: Foo)
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
}
