/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.coroutines

import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.*
import io.kotest.matchers.booleans.*
import io.kotest.matchers.types.*
import kotlinx.coroutines.*
import org.junit.*

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
    @Provide val customContext = NamedCoroutineContext<MyScope>(Dispatchers.Main)
    val coroutineScope = inject<NamedCoroutineScope<MyScope>>()
    coroutineScope.coroutineContext.minusKey(Job.Key) shouldBeSameInstanceAs customContext.value
  }
}
