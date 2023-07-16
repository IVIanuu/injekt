/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.inject
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import org.junit.Test

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
        scope.invoke {
          holder.inc()
        }
      }
    }

    jobs.forEach { it.start() }
    jobs.forEach { it.cancelAndJoin() }

    holder.get() shouldBe 1
  }

  @Test fun testScopedTag() {
    var callCount = 0

    class Foo

    @Provide fun scopedFoo(): @Scoped<TestScope> Foo {
      callCount++
      return Foo()
    }

    @Provide val scope = Scope<TestScope>()
    callCount shouldBe 0
    val a = inject<Foo>()
    callCount shouldBe 1
    val b = inject<Foo>()
    callCount shouldBe 1
    a shouldBeSameInstanceAs b
  }
}

private object TestScope
