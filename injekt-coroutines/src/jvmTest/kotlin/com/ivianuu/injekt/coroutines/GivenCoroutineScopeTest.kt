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

package com.ivianuu.injekt.coroutines

import com.ivianuu.injekt.*
import com.ivianuu.injekt.scope.*
import io.kotest.matchers.booleans.*
import io.kotest.matchers.types.*
import kotlinx.coroutines.*
import org.junit.*

@Providers(
  "com.ivianuu.injekt.common.*",
  "com.ivianuu.injekt.scope.*"
)
class GivenCoroutineScopeTest {
  @Test fun testGivenCoroutineScopeLifecycle() {
    val scope = inject<TestScope1>()
    val coroutineScope = scope.coroutineScope
    coroutineScope.isActive.shouldBeTrue()
    scope.dispose()
    coroutineScope.isActive.shouldBeFalse()
  }

  @Test fun testGivenCoroutineScopeAccessors() {
    val scope = inject<TestScope1>()
    val a = scope.coroutineScope
    val b = scope.element<CoroutineScope>()
    val c = scope.element<GivenCoroutineScope<TestScope1>>()
    a shouldBeSameInstanceAs b
    b shouldBeSameInstanceAs c
  }

  @OptIn(ExperimentalStdlibApi::class)
  @Test fun testCanSpecifyCustomCoroutineContext() {
    @Provide val customContext: GivenCoroutineContext<TestScope1> = Dispatchers.Main
    val scope = inject<TestScope1>()
    scope.coroutineScope.coroutineContext[CoroutineDispatcher] shouldBeSameInstanceAs customContext
  }
}

typealias TestScope1 = Scope
