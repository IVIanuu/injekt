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

import com.ivianuu.injekt.test.*
import io.kotest.matchers.*
import io.kotest.matchers.nulls.*
import io.kotest.matchers.types.*
import org.jetbrains.kotlin.name.*
import org.junit.*

class GivenDeclarationTest {
  @Test fun testGivenFunction() = singleAndMultiCodegen(
    """
            @Given fun foo() = Foo()
    """,
    """
        fun invoke() = given<Foo>() 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testGivenProperty() = singleAndMultiCodegen(
    """
            @Given val foo = Foo()
    """,
    """
        fun invoke() = given<Foo>() 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testGivenClass() = singleAndMultiCodegen(
    """
            @Given val foo = Foo()
            @Given class Dep(@Given val foo: Foo)
    """,
    """
      fun invoke() = given<Dep>() 
    """
  ) {
    invokeSingleFile<Any>().javaClass.name shouldBe "com.ivianuu.injekt.integrationtests.Dep"
  }

  @Test fun testGivenClassPrimaryConstructor() = singleAndMultiCodegen(
    """
            @Given val foo = Foo()
            class Dep @Given constructor(@Given val foo: Foo)
    """,
    """
        fun invoke() = given<Dep>() 
    """
  ) {
    invokeSingleFile<Any>().javaClass.name shouldBe "com.ivianuu.injekt.integrationtests.Dep"
  }

  @Test fun testGivenClassSecondaryConstructor() = singleAndMultiCodegen(
    """
            @Given val foo = Foo()
            class Dep {
                @Given constructor(@Given foo: Foo)
            }
    """,
    """
        fun invoke() = given<Dep>() 
    """
  ) {
    invokeSingleFile<Any>().javaClass.name shouldBe "com.ivianuu.injekt.integrationtests.Dep"
  }

  @Test fun testGivenClassWithMultipleGivenConstructors() = singleAndMultiCodegen(
    """
            class Dep {
                @Given constructor(@Given foo: Foo)
                @Given constructor(@Given bar: Bar)
            }
            @Given val foo = Foo()
    """,
    """
        fun invoke() = given<Dep>() 
    """
  )

  @Test fun testNestedGivenClass() = singleAndMultiCodegen(
    """
            @Given val foo = Foo()
            class Outer {
                @Given class Dep(@Given val foo: Foo)
            }
    """,
    """
            @GivenImports("com.ivianuu.injekt.integrationtests.Outer.Dep")
      fun invoke() = given<Outer.Dep>() 
    """
  ) {
    invokeSingleFile<Any>().javaClass.name shouldBe "com.ivianuu.injekt.integrationtests.Outer\$Dep"
  }

  @Test fun testGivenObject() = singleAndMultiCodegen(
    """
            @Given val foo = Foo()
            @Given object Dep {
                init {
                    given<Foo>()
                }
            }
    """,
    """
        fun invoke() = given<Dep>() 
    """
  ) {
    invokeSingleFile<Any>().javaClass.name shouldBe "com.ivianuu.injekt.integrationtests.Dep"
  }

  @Test fun testGivenCompanionObject() = singleAndMultiCodegen(
    """
            @Given val foo = Foo()
            class Dep {
                @Given companion object {
                    init {
                        given<Foo>()
                    }
                }
            }
    """,
    """
        fun invoke() = given<Dep.Companion>() 
    """
  ) {
    invokeSingleFile<Any>().javaClass.name shouldBe "com.ivianuu.injekt.integrationtests.Dep\$Companion"
  }

  @Test fun testGivenExtensionFunction() = singleAndMultiCodegen(
    """
            @Given fun Foo.bar() = Bar(this)
    """,
    """
           fun invoke(@Given foo: Foo) = given<Bar>() 
    """
  ) {
    invokeSingleFile(Foo())
      .shouldBeTypeOf<Bar>()
  }

  @Test fun testGivenExtensionProperty() = singleAndMultiCodegen(
    """
            @Given val Foo.bar get() = Bar(this)
    """,
    """
           fun invoke(@Given foo: Foo) = given<Bar>() 
    """
  ) {
    invokeSingleFile(Foo())
      .shouldBeTypeOf<Bar>()
  }

  @Test fun testGivenValueParameter() = codegen(
    """
            fun invoke(@Given foo: Foo) = given<Foo>()
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testGivenLocalVariable() = codegen(
    """
            fun invoke(foo: Foo): Foo {
                @Given val givenFoo = foo
                return given()
            }
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testGivenConstructorParameterInFieldInitializer() = singleAndMultiCodegen(
    """
            class MyClass(@Given foo: Foo) {
                val foo = given<Foo>()
            }
    """,
    """
           fun invoke(@Given foo: Foo) = MyClass().foo 
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testGivenConstructorParameterInClassInitializer() = singleAndMultiCodegen(
    """
            class MyClass(@Given foo: Foo) {
                val foo: Foo
                init {
                    this.foo = given()
                }
            }
    """,
    """
           fun invoke(@Given foo: Foo) = MyClass().foo 
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testGivenConstructorParameterInConstructorBody() = singleAndMultiCodegen(
    """
            class MyClass {
                val foo: Foo
                constructor(@Given foo: Foo) {
                    this.foo = given()   
                }
            }
    """,
    """
           fun invoke(@Given foo: Foo) = MyClass().foo 
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testImportedGivenFunctionInObject() = singleAndMultiCodegen(
    """
            object FooGivens {
                @Given fun foo() = Foo()
            }
    """,
    """
            @GivenImports("com.ivianuu.injekt.integrationtests.FooGivens.foo")
      fun invoke() = given<Foo>() 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testImportedGivenFunctionInObjectWithStar() = singleAndMultiCodegen(
    """
            object FooGivens {
                @Given fun foo() = Foo()
            }
    """,
    """
            @GivenImports("com.ivianuu.injekt.integrationtests.FooGivens.*")
      fun invoke() = given<Foo>() 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testImportedGivenFunctionInCompanionObject() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
      """
                        class FooGivens {
                            companion object {
                                @Given fun foo() = Foo()
                            }
                        }
                """,
          packageFqName = FqName("givens")
        )
      ),
      listOf(
        invokableSource(
      """
                        @GivenImports("givens.FooGivens")
                  fun invoke() = given<Foo>()
                """
        )
      )
    )
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testGivenLambdaReceiverParameter() = singleAndMultiCodegen(
    """
            inline fun <T, R> diyWithGiven(value: T, block: @Given T.() -> R) = block(value)
    """,
    """
            fun invoke(foo: Foo): Foo {
                return diyWithGiven(foo) { given<Foo>() }
            } 
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testGivenLambdaParameterDeclarationSite() = singleAndMultiCodegen(
    """
            inline fun <T, R> withGiven(value: T, block: (@Given T) -> R) = block(value)
    """,
    """
            fun invoke(foo: Foo): Foo {
                return withGiven(foo) { given<Foo>() }
            } 
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testGivenLambdaParameterDeclarationSiteWithTypeAlias() = singleAndMultiCodegen(
    """
            typealias UseContext<T, R> = (@Given T) -> R
            inline fun <T, R> withGiven(value: T, block: UseContext<T, R>) = block(value)
    """,
    """
            fun invoke(foo: Foo): Foo {
                return withGiven(foo) { given<Foo>() }
            } 
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testCanLeaveOutGivenLambdaParameters() = singleAndMultiCodegen(
    """
            val lambda: (@Given Foo) -> Foo = { given<Foo>() }
    """,
    """
            fun invoke(@Given foo: Foo): Foo {
                return lambda()
            }
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testCanLeaveOutGivenLambdaParametersWithTypeAlias() = singleAndMultiCodegen(
    """
            typealias LambdaType = (@Given Foo) -> Foo
            val lambda: LambdaType = { given<Foo>() }
    """,
    """
            fun invoke(@Given foo: Foo): Foo {
                return lambda()
            } 
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testGivenLambdaParameterUseSite() = singleAndMultiCodegen(
    """
            inline fun <T, R> withGiven(value: T, block: (T) -> R) = block(value)
    """,
    """
            fun invoke(foo: Foo): Foo {
                return withGiven(foo) { foo: @Given Foo -> given<Foo>() }
            } 
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testGivenInNestedBlock() = codegen(
    """
            fun invoke(a: Foo, b: Foo): Pair<Foo, Foo> {
                return run {
                    @Given val givenA = a
                    given<Foo>() to run {
                        @Given val givenB = b
                        given<Foo>()
                    }
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

  @Test fun testGivenInTheMiddleOfABlock() = codegen(
    """
            fun invoke(provided: Foo): Pair<Foo?, Foo?> {
                val a = givenOrNull<Foo>()
                @Given val given = provided
                val b = givenOrNull<Foo>()
                return a to b
            }
    """
  ) {
    val foo = Foo()
    val result = invokeSingleFile<Pair<Foo?, Foo?>>(foo)
    result.first.shouldBeNull()
    result.second shouldBeSameInstanceAs foo
  }

  @Test fun testGivenLocalClass() = codegen(
    """
            fun invoke(_foo: Foo): Foo {
                @Given class FooProvider(@Given __foo: Foo = _foo) {
                    val foo = __foo
                }
                return given<FooProvider>().foo
            }
    """
  ) {
    val foo = Foo()
    foo shouldBeSameInstanceAs invokeSingleFile(foo)
  }

  @Test fun testGivenLocalFunction() = codegen(
    """
            fun invoke(foo: Foo): Foo {
                @Given fun foo() = foo
                return given<Foo>()
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
                @Given val instance = object : A, B  {
                }
                given<A>()
                given<B>()
            }
    """
  )

  @Test fun testGivenSuspendFunction() = singleAndMultiCodegen(
    """
            @Given suspend fun foo() = Foo()
    """,
    """
        fun invoke() = runBlocking { given<Foo>() } 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testGivenComposableFunction() = singleAndMultiCodegen(
    """
            @Given @Composable fun foo() = Foo()
    """,
    """
           @Composable fun invoke() { given<Foo>() } 
    """
  )

  @Test fun testGivenSuperClassPrimaryConstructorParameter() = singleAndMultiCodegen(
    """
            abstract class MySuperClass(@Given val foo: Foo)
    """,
    """
            @Given object MySubClass : MySuperClass(Foo())
      fun invoke() = given<Foo>() 
    """
  )
}
