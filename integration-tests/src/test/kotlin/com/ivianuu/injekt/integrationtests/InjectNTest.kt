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
import org.junit.Test

class InjectNTest {
  @Test fun testInjectNFunction() = codegen(
    """
      @Inject1<String> fun myFunc() {
        inject<String>()
      }

      fun invoke(@Inject string: String) {
        myFunc()
      }
    """
  )

  // todo property

  // todo class

  // todo primary constructor

  // todo secondary constructor

  // todo suspend function

  // todo composable function

  // todo lambda

  @Test fun testInjectNLambda() = codegen(
    """
      val lambda: @Inject1<Unit> () -> Unit = { inject<Unit>() }

      fun invoke(@Inject string: String) {
        lambda()
      }
    """
  )

  // todo suspend lambda

  // todo composable lambda

  // todo fun interface

  // todo suspend fun interface

  // todo composable fun interface
}
