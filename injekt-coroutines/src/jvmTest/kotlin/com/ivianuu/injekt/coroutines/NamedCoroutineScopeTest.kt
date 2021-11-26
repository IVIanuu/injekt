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

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.Scope
import com.ivianuu.injekt.inject
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import org.junit.Test

class NamedCoroutineScopeTest {
  private object MyScope

  @Test fun testComponentScopeLifecycle() {
    @Provide val scope = Scope<MyScope>()
    val coroutineScope = inject<NamedCoroutineScope<MyScope>>()
    coroutineScope.isActive.shouldBeTrue()
    scope.dispose()
    coroutineScope.isActive.shouldBeFalse()
  }

  @OptIn(ExperimentalStdlibApi::class)
  @Test fun testCanSpecifyCustomCoroutineContext() {
    @Provide val scope = Scope<MyScope>()
    @Provide val customContext: NamedCoroutineContext<MyScope> = Dispatchers.Main
    val coroutineScope = inject<NamedCoroutineScope<MyScope>>()
    coroutineScope.coroutineContext[CoroutineDispatcher] shouldBeSameInstanceAs customContext
  }
}
