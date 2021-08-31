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

import com.ivianuu.injekt.test.Bar
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokableSource
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.singleAndMultiCodegen
import com.ivianuu.injekt.test.source
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldBeTypeOf
import org.jetbrains.kotlin.name.FqName
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

  @Test fun testProvideValueParameter() = codegen(
    """
      fun invoke(@Provide foo: Foo) = inject<Foo>()
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testInjectValueParameter() = codegen(
    """
      fun invoke(@Inject foo: Foo) = inject<Foo>()
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
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
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
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
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testInjectPrimaryConstructorParameterInClassInitializer() = singleAndMultiCodegen(
    """
      class MyClass(@Inject foo: Foo) {
        val foo: Foo
        init {
          this.foo = inject()
        }
      }
    """,
    """
      fun invoke(@Inject foo: Foo) = MyClass().foo 
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testInjectPrimaryConstructorParameterInClassBody() = singleAndMultiCodegen(
    """
      class MyClass(@Inject foo: Foo) {
        val foo: Foo = inject()
      }
    """,
    """
      fun invoke(@Inject foo: Foo) = MyClass().foo 
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testClassDeclarationInClassBody() = singleAndMultiCodegen(
    """
      class MyClass(private val _foo: Foo) {
        val foo: Foo = inject()
        @Provide fun foo() = _foo
      }
    """,
    """
      fun invoke(@Inject foo: Foo) = MyClass(foo).foo 
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testInjectConstructorParameterInConstructorBody() = singleAndMultiCodegen(
    """
      class MyClass {
        val foo: Foo
        constructor(@Inject foo: Foo) {
          this.foo = inject()   
        }
      }
    """,
    """
       fun invoke(@Inject foo: Foo) = MyClass().foo 
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testImportedProvideFunctionInObject() = singleAndMultiCodegen(
    """
      object FooModule {
        @Provide fun foo() = Foo()
      }
    """,
    """
      @Providers("com.ivianuu.injekt.integrationtests.FooModule.foo")
      fun invoke() = inject<Foo>() 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testImportedProvideFunctionInObjectWithStar() = singleAndMultiCodegen(
    """
      object FooModule {
        @Provide fun foo() = Foo()
      }
    """,
    """
      @Providers("com.ivianuu.injekt.integrationtests.FooModule.*")
      fun invoke() = inject<Foo>() 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testImportedOProvideFunctionInCompanionObject() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            class FooModule {
              companion object {
                @Provide fun foo() = Foo()
              }
            }
          """,
          packageFqName = FqName("providers")
        )
      ),
      listOf(
        invokableSource(
          """
            @Providers("providers.FooModule")
            fun invoke() = inject<Foo>()
          """
        )
      )
    )
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
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
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testInjectLambdaParameterDeclarationSiteWithTypeAlias() = singleAndMultiCodegen(
    """
      typealias UseContext<T, R> = (@Inject T) -> R
      inline fun <T, R> withProvidedInstance(value: T, block: UseContext<T, R>) = block(value)
    """,
    """
      fun invoke(foo: Foo) = withProvidedInstance(foo) { inject<Foo>() }
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
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
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testCanLeaveOutInjectLambdaParametersWithTypeAlias() = singleAndMultiCodegen(
    """
      typealias LambdaType = (@Inject Foo) -> Foo
      val lambda: LambdaType = { inject<Foo>() }
    """,
    """
      fun invoke(@Inject foo: Foo) = lambda()
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
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
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
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
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
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
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testProvideInNestedBlock() = codegen(
    """
      fun invoke(a: Foo, b: Foo) = run {
        @Provide val providedA = a
        inject<Foo>() to run {
          @Provide val providedB = b
          inject<Foo>()
        }
      }
    """
  ) {
    val a = Foo()
    val b = Foo()
    val result = invokeSingleFile<Pair<Foo, Foo>>(a, b)
    a shouldBeSameInstanceAs result.first
    b shouldBeSameInstanceAs result.second
  }

  @Test fun testProvideInTheMiddleOfABlock() = codegen(
    """
      fun invoke(foo: Foo): Pair<Foo?, Foo?> {
        val a = injectOrNull<Foo>()
        @Provide val provided = foo
        val b = injectOrNull<Foo>()
        return a to b
      }
    """
  ) {
    val foo = Foo()
    val result = invokeSingleFile<Pair<Foo?, Foo?>>(foo)
    result.first.shouldBeNull()
    result.second shouldBeSameInstanceAs foo
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
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
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
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
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
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
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
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

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
      @Composable fun invoke() = inject<Foo>() 
    """
  )

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
