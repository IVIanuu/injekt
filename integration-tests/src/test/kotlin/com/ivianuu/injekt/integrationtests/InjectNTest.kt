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
import com.ivianuu.injekt.test.singleAndMultiCodegen
import org.junit.Test

class InjectNTest {
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

  // todo property getter / setter

  // todo class

  // todo primary constructor

  // todo secondary constructor

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

  @Test fun testInjectNLambda() = codegen(
    """
      val lambda: @Inject1<Unit> () -> Unit = { inject<Unit>() }

      fun invoke(@Inject string: String) {
        lambda()
        lambda.invoke()
      }
    """
  )

  @Test fun testInjectNSuspendLambda() = codegen(
    """
      val lambda: @Inject1<Unit> suspend () -> Unit = { inject<Unit>() }

      suspend fun invoke(@Inject string: String) {
        lambda()
        lambda.invoke()
      }
    """
  )


  @Test fun testInjectNComposableLambda() = codegen(
    """
      val lambda: @Inject1<Unit> @Composable () -> Unit = { inject<Unit>() }

      @Composable fun invoke(@Inject string: String) {
        lambda()
        lambda.invoke()
      }
    """
  )

  @Test fun testInjectNFunInterface() = codegen(
    """
      fun interface MyType {
        @Inject1<Unit> operator fun invoke()
      }
      val lambda: MyType = MyType { inject<Unit>() }

      fun invoke(@Inject string: String) {
        lambda()
        lambda.invoke()
      }
    """
  )

  @Test fun testInjectNSuspendFunInterface() = codegen(
    """
      fun interface MyType {
        @Inject1<Unit> suspend operator fun invoke()
      }
      val lambda: MyType = MyType { inject<Unit>() }

      suspend fun invoke(@Inject string: String) {
        lambda()
        lambda.invoke()
      }
    """
  )

  @Test fun testInjectNComposableFunInterface() = codegen(
    """
      fun interface MyType {
        @Inject1<Unit> @Composable operator fun invoke()
      }
      val lambda: MyType = MyType { inject<Unit>() }

      @Composable fun invoke(@Inject string: String) {
        lambda()
        lambda.invoke()
      }
    """
  )
}
