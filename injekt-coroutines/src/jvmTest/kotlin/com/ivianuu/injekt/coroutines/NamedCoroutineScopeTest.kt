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
import com.ivianuu.injekt.container.*
import io.kotest.matchers.booleans.*
import io.kotest.matchers.types.*
import kotlinx.coroutines.*
import org.junit.*

class InjectCoroutineScopeTest {
  @Test fun testCoroutineScopeElementLifecycle() {
    @Provide val container = inject<Container<AppScope>>()
    val coroutineScope = container.element<NamedCoroutineScope<AppScope>>()
    coroutineScope.isActive.shouldBeTrue()
    container.dispose()
    coroutineScope.isActive.shouldBeFalse()
  }

  @OptIn(ExperimentalStdlibApi::class)
  @Test fun testCanSpecifyCustomCoroutineContext() {
    @Provide val customContext: NamedCoroutineContext<AppScope> = Dispatchers.Main
    @Provide val container = inject<Container<AppScope>>()
    val scope = container.element<NamedCoroutineScope<AppScope>>()
    scope.coroutineContext[CoroutineDispatcher] shouldBeSameInstanceAs customContext
  }
}
