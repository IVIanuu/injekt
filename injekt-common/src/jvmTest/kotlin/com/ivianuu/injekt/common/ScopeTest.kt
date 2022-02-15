/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.*
import io.kotest.matchers.*
import io.kotest.matchers.types.*
import kotlinx.atomicfu.*
import kotlinx.coroutines.*
import org.junit.*

class ScopeTest {
  @Test fun testScope() {
    val scope = Scope<TestScope>()
    scope { "a" } shouldBe "a"
    scope { "b" } shouldBe "a"
  }

  @Test fun scopeConcurrencyStressTest() = runBlocking(
    newFixedThreadPoolContext(64, "ctx")
  ) {
    val scope = Scope<TestScope>()

    class CallCountHolder {
      private val _callCount = atomic(0)
      fun get() = _callCount.value
      fun inc() = _callCount.update { it.inc() }
    }

    val holder = CallCountHolder()

    val jobs = (1..64).map {
      launch(start = CoroutineStart.LAZY) {
        scope {
          holder.inc()
        }
      }
    }

    jobs.forEach { it.start() }
    jobs.forEach { it.cancelAndJoin() }

    holder.get() shouldBe 1
  }

  @Test fun testDispose() {
    val scope = Scope<TestScope>()
    var disposeCalls = 0
    scope {
      Disposable {
        disposeCalls++
      }
    }
    disposeCalls shouldBe 0
    scope.dispose()
    disposeCalls shouldBe 1
    scope.dispose()
    disposeCalls shouldBe 1
  }
}
