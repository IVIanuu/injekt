/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.inject
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import org.junit.Test

class NamedCoroutineScopeTest {
  private object MyScope

  @Test fun testNamedScopeLifecycle() {
    @Provide val scope = Scope<MyScope>()
    val coroutineScope = inject<NamedCoroutineScope<MyScope>>()
    coroutineScope.isActive.shouldBeTrue()
    scope.dispose()
    coroutineScope.isActive.shouldBeFalse()
  }

  @Test fun testCanSpecifyCustomCoroutineContext() {
    @Provide val scope = Scope<MyScope>()
    @Provide val customContext: NamedCoroutineContext<MyScope> = Dispatchers.Main
    val coroutineScope = inject<NamedCoroutineScope<MyScope>>()
    coroutineScope.coroutineContext.minusKey(Job.Key) shouldBeSameInstanceAs customContext
  }
}
