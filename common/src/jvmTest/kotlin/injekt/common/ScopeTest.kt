/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.common

import injekt.*
import io.kotest.matchers.*
import io.kotest.matchers.types.*
import kotlinx.atomicfu.*
import kotlinx.coroutines.*
import org.junit.*

class ScopeTest {
  @Test fun testScope() {
    val scope = Scope<TestScope>()
    scope.get("") { "a" } shouldBe "a"
    scope.get("") { "b" } shouldBe "a"
  }

  @Test fun scopeConcurrencyStressTest(): Unit = runBlocking(
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
        scope.get("") {
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
    val a = create<Foo>()
    callCount shouldBe 1
    val b = create<Foo>()
    callCount shouldBe 1
    a shouldBeSameInstanceAs b
  }

  @Test fun testDispose() {
    var disposeCalls = 0
    val scope = Scope<TestScope>()
    scope.get("") {
      ScopeDisposable { disposeCalls++ }
    }
    disposeCalls shouldBe 0
    scope.dispose()
    disposeCalls shouldBe 1
    scope.dispose()
    disposeCalls shouldBe 1
  }
}

private object TestScope
