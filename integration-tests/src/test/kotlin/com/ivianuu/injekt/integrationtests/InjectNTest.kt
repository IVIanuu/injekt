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

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.singleAndMultiCodegen
import io.kotest.matchers.shouldBe
import org.junit.Test

class InjectNTest {
  @Test fun testHehe() = codegen(
    """
      typealias DbContext = Inject2<Int, String>

      @Inject1<String> val counterDb: String get() = inject()

      @DbContext suspend inline fun <R> dbTransaction(crossinline block: @DbContext suspend () -> R): R {
        return block()
      }

      @DbContext suspend fun decCounter() {
        dbTransaction {
          counterDb.toString()
        }
      }
    """
  )

  @Test fun testInjectNFunction() = singleAndMultiCodegen(
    """
      @Inject1<String> fun myFunc() {
        inject<String>()
      }
    """,
    """
      fun invoke(@Inject string: String) {
        myFunc()
      }
    """
  )

  @Test fun testInjectNGenericFunction() = singleAndMultiCodegen(
    """
      @Inject1<T> fun <T> myFunc() {
        inject<T>()
      }
    """,
    """
      fun invoke(@Inject string: String) {
        myFunc<String>()
      }
    """
  )

  @Test fun testInjectNProperty() = singleAndMultiCodegen(
    """
      @Inject1<String> val myProperty: String get() = inject()
    """,
    """
      fun invoke(@Inject string: String) {
        myProperty
      }
    """
  )

  @Test fun testInjectNGenericProperty() = singleAndMultiCodegen(
    """
      @Inject1<List<T>> val <T> T.myProperty: List<T> get() = inject()
    """,
    """
      fun invoke(@Inject string: String) {
        @Provide val stringList = listOf<String>()
        string.myProperty
      }
    """
  )

  // todo property getter / setter

  @Test fun testInjectNClass() = singleAndMultiCodegen(
    """
      @Inject1<String> @Provide class MyClass @Inject1<Unit> constructor(val int: Int) {
        val string = inject<String>()
        fun string(): String = inject<String>()
        val string2: String get() = inject<String>()
      }
    """,
    """
      fun invoke(@Inject string: String): Pair<String, String> {
        @Provide val unit = Unit
        @Provide val int = 0
        return MyClass().string to inject<MyClass>().string()
      }
    """
  ) {
    val (a, b) = invokeSingleFile<Pair<String, String>>("42")
    a shouldBe "42"
    b shouldBe "42"
  }

  @Test fun testInjectNGenericClass() = singleAndMultiCodegen(
    """
      @Inject1<T> @Provide class MyClass<T> {
        val string = inject<T>()
        fun string() = inject<T>()
        val string2 get() = inject<T>()
      }
    """,
    """
      fun invoke(@Inject string: String) {
        MyClass<String>()
        inject<MyClass<String>>()
      }
    """
  )

  @Test fun testInjectNSuperClass() = singleAndMultiCodegen(
    """
      @Inject1<String> abstract class MyAbstractClass @Inject1<Unit> constructor(@Inject val int: Int)
      @Inject1<String> @Provide class MyClass @Inject1<Int> constructor(val unit: Unit) : MyAbstractClass()
    """,
    """
      fun invoke(@Inject string: String) {
        @Provide val unit = Unit
        @Provide val int = 0
        MyClass()
        inject<MyClass>()
      }
    """
  )

  @Test fun testInjectNGenericSuperClass() = singleAndMultiCodegen(
    """
      @Inject1<T> abstract class MyAbstractClass<T>(@Inject val unit: Unit) {
        
      }
    """,
    """
      @Inject1<T> @Provide class MyClass<T>(unit: Unit) : MyAbstractClass<T>()
    """,
    """
      fun invoke(@Inject string: String) {
        @Provide val unit = Unit
        MyClass<String>()
        inject<String>()
      }
    """
  )

  @Test fun testInjectNPrimaryConstructor() = singleAndMultiCodegen(
    """
      class MyClass @Inject1<String> constructor() {
        val string = inject<String>()
      }
    """,
    """
      fun invoke(@Inject string: String) {
        MyClass()
      }
    """
  )

  @Test fun testInjectNSecondaryConstructor() = singleAndMultiCodegen(
    """
      class MyClass {
        @Inject1<String> constructor() {
          val string = inject<String>()
        }
      }
    """,
    """
      fun invoke(@Inject string: String) {
        MyClass()
      }
    """
  )

  @Test fun testInjectNSuspendFunction() = singleAndMultiCodegen(
    """
      @Inject1<String> suspend fun myFunc() {
        inject<String>()
      }
    """,
    """
      suspend fun invoke(@Inject string: String) {
        myFunc()
      }
    """
  )

  @Test fun testInjectNComposableFunction() = singleAndMultiCodegen(
    """
      @Inject1<String> @Composable fun myFunc() {
        inject<String>()
      }
    """,
    """
      @Composable fun invoke(@Inject string: String) {
        myFunc()
      }
    """
  )

  @Test fun testInjectNComposableProperty() = singleAndMultiCodegen(
    """
      @Inject1<String> val myProperty: String @Composable get() = inject()
    """,
    """
      @Composable fun invoke(@Inject string: String) {
        myProperty
      }
    """
  )

  @Test fun testInjectNLambda() = singleAndMultiCodegen(
    """
      val lambda: @Inject1<String> () -> String = { inject<String>() }
    """,
    """
      fun invoke(@Inject string: String) {
        lambda()
        lambda.invoke()
      }
    """
  )

  @Test fun testInjectNGenericLambda() = singleAndMultiCodegen(
    """
      fun <T> lambda(): @Inject1<T> () -> T = { inject<T>() }
    """,
    """
      fun invoke(@Inject string: String) {
        lambda<String>()()
        lambda<String>().invoke()
      }
    """
  )

  @Test fun testInjectNSuspendLambda() = singleAndMultiCodegen(
    """
      val lambda: @Inject1<String> suspend () -> String = { inject<String>() }
    """,
    """
      suspend fun invoke(@Inject string: String) {
        lambda()
        lambda.invoke()
      }
    """
  )


  @Test fun testInjectNComposableLambda() = singleAndMultiCodegen(
    """
      val lambda: @Inject1<String> @Composable () -> String = { inject<String>() }
    """,
    """
      @Composable fun invoke(@Inject string: String) {
        lambda()
        lambda.invoke()
      }
    """
  )

  @Test fun testInjectNFunInterface() = singleAndMultiCodegen(
    """
      fun interface MyType {
        @Inject1<String> operator fun invoke(): String
      }
      val lambda: MyType = MyType { inject<String>() }
    """,
    """
      fun invoke(@Inject string: String) {
        lambda()
        lambda.invoke()
      }
    """
  )

  @Test fun testInjectNSuspendFunInterface() = singleAndMultiCodegen(
    """
      fun interface MyType {
        @Inject1<String> suspend operator fun invoke(): String
      }
      val lambda: MyType = MyType { inject<String>() }
    """,
    """
      suspend fun invoke(@Inject string: String) {
        lambda()
        lambda.invoke()
      }
    """
  )

  @Test fun testInjectNComposableFunInterface() = singleAndMultiCodegen(
    """
      fun interface MyType {
        @Inject1<String> @Composable operator fun invoke(): String
      }
      val lambda: MyType = MyType { inject<String>() }
    """,
    """
      @Composable fun invoke(@Inject string: String) {
        lambda()
        lambda.invoke()
      }
    """
  )
}
