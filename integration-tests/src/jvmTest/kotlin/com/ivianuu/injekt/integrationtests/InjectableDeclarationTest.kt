/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

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
      fun invoke() = context<Foo>() 
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
      fun invoke() = context<Foo>() 
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
      fun invoke() = context<Dep>() 
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
      fun invoke() = context<Dep>() 
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
      fun invoke() = context<Dep>() 
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
      fun invoke() = context<(Foo) -> Dep>() to context<(Bar) -> Dep>()
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
      fun invoke() = context<Outer.Dep>() 
    """
  ) {
    invokeSingleFile<Any>().javaClass.name shouldBe "com.ivianuu.injekt.integrationtests.Outer\$Dep"
  }

  @Test fun testProvideObject() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide object Dep {
        init {
          context<Foo>()
        }
      }
    """,
    """
      fun invoke() = context<Dep>() 
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
            context<Foo>()
          }
        }
      }
    """,
    """
      fun invoke() = context<Dep.Companion>() 
    """
  ) {
    invokeSingleFile<Any>().javaClass.name shouldBe "com.ivianuu.injekt.integrationtests.Dep\$Companion"
  }

  @Test fun testProvideFunctionExtensionReceiver() = singleAndMultiCodegen(
    """
      fun @receiver:Provide Foo.bar() = Bar(context())
    """,
    """
      fun invoke() = provide(Foo()) { bar() }
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Bar>()
  }

  @Test fun testProvideFunctionContextReceiver() = singleAndMultiCodegen(
    """
      context((@Provide Foo)) fun bar() = Bar(context())
    """,
    """
      fun invoke() = provide(Foo()) { bar() }
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Bar>()
  }

  @Test fun testProvidePropertyExtensionReceiver() = singleAndMultiCodegen(
    """
      val @receiver:Provide Foo.bar get() = Bar(context())
    """,
    """
      fun invoke() = provide(Foo()) { bar }
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Bar>()
  }

  @Test fun testProvidePropertyContextReceiver() = singleAndMultiCodegen(
    """
      context((@Provide Foo)) val bar get() = Bar(context())
    """,
    """
      fun invoke() = provide(Foo()) { bar }
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Bar>()
  }

  @Test fun testProvideClassContextReceiver() = singleAndMultiCodegen(
    """
      context((@Provide Foo)) class Dep {
        fun resolve() = Bar(context<Foo>())
      }
    """,
    """
      fun invoke() = provide(Foo()) { Dep().resolve() }
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Bar>()
  }

  @Test fun testProvideContextFunction() = singleAndMultiCodegen(
    """
      context(Foo) @Provide fun bar() = Bar(this@Foo)
    """,
    """
      fun invoke(@Provide foo: Foo) = context<Bar>()
    """
  ) {
    invokeSingleFile(Foo())
      .shouldBeTypeOf<Bar>()
  }

  @Test fun testProvideContextProperty() = singleAndMultiCodegen(
    """
      context(Foo) @Provide val bar get() = Bar(this@Foo)
    """,
    """
      fun invoke(@Provide foo: Foo) = context<Bar>() 
    """
  ) {
    invokeSingleFile(Foo())
      .shouldBeTypeOf<Bar>()
  }

  @Test fun testProvideContextClass() = singleAndMultiCodegen(
    """
      context(Foo) @Provide class Dep
    """,
    """
      fun invoke(@Provide foo: Foo) = context<Dep>()
    """
  ) {
    invokeSingleFile(Foo())
  }

  // todo @Test
  fun testProvideContextConstructor() = singleAndMultiCodegen(
    """
      @Provide class Dep(foo: Any) {
        context(Foo) constructor() : this(this@Foo)
      }
    """,
    """
      fun invoke(@Provide foo: Foo) = context<Dep>() 
    """
  ) {
    invokeSingleFile(Foo())
  }

  @Test fun testProvideValueParameter() = codegen(
    """
      fun invoke(@Provide foo: Foo) = context<Foo>()
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testProvideLocalVariable() = codegen(
    """
      fun invoke(foo: Foo): Foo {
        @Provide val providedFoo = foo
        return context()
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
        return context()
      }
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
      fun invoke(foo: Foo) = withProvidedInstance(foo) { foo: @Provide Foo -> context<Foo>() }
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testProvideLocalClass() = codegen(
    """
      fun invoke(@Provide _foo: Foo): Foo {
        @Provide class FooProvider(__foo: Foo = _foo) {
          val foo = __foo
        }
        return context<FooProvider>().foo
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
        return context<Foo>()
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
        context<A>()
        context<B>()
      }
    """
  )

  @Test fun testSuperClassPrimaryProvideConstructorParameter() = singleAndMultiCodegen(
    """
      abstract class MySuperClass(@property:Provide val foo: Foo)
    """,
    """
      @Provide object MySubClass : MySuperClass(Foo())
      fun invoke() = context<Foo>()
    """
  )

  @Test fun testProvideFunctionInLocalClass() = codegen(
    """
      fun invoke() {
        class MyClass {
          @Provide fun foo() = Foo()
          
          override fun equals(other: Any?): Boolean {
            context<Foo>()
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
            context<Foo>()
            return super.equals(other)
          }
        }
      }
    """
  )
}
