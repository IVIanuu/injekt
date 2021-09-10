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
import com.ivianuu.injekt.inject
import com.ivianuu.injekt.scope.AppScope
import com.ivianuu.injekt.scope.DisposableScope
import com.ivianuu.injekt.scope.requireElement
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import org.junit.Test

class InjektCoroutineScopeTest {
  @Test fun testCoroutineScopeElementLifecycle() {
    @Provide val scope = inject<AppScope>()
    val coroutineScope = requireElement<InjektCoroutineScope<AppScope>>()
    coroutineScope.isActive.shouldBeTrue()
    (scope as DisposableScope).dispose()
    coroutineScope.isActive.shouldBeFalse()
  }

  @OptIn(ExperimentalStdlibApi::class)
  @Test fun testCanSpecifyCustomCoroutineContext() {
    @Provide val customContext: InjektCoroutineContext<AppScope> = Dispatchers.Main
    @Provide val scope = inject<AppScope>()
    val coroutineScope = requireElement<InjektCoroutineScope<AppScope>>()
    coroutineScope.coroutineContext[CoroutineDispatcher] shouldBeSameInstanceAs customContext
  }
}
