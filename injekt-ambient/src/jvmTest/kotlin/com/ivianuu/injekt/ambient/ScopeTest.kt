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

package com.ivianuu.injekt.ambient

import io.kotest.matchers.*
import io.kotest.matchers.booleans.*
import org.junit.*

class ScopeTest {
  @Test fun testGetSet() {
    val scope = DisposableScope()
    scope.get<String>(0) shouldBe null
    scope.set(0, "value")
    scope.get<String>(0) shouldBe "value"
  }

  @Test fun testRemoveDisposesValue() {
    val scope = DisposableScope()
    var disposed = false
    scope.set(0, ScopeDisposable { disposed = true })

    disposed.shouldBeFalse()
    scope.remove(0)
    disposed.shouldBeTrue()
  }

  @Test fun testSetDisposesOldValue() {
    val scope = DisposableScope()
    var disposed = false
    scope.set(0, ScopeDisposable { disposed = true })

    disposed.shouldBeFalse()
    scope.set(0, 0)
    disposed.shouldBeTrue()
  }

  @Test fun testDisposeDisposesValues() {
    val scope = DisposableScope()
    var disposed = false
    scope.set(0, ScopeDisposable { disposed = true })

    disposed.shouldBeFalse()
    scope.dispose()
    disposed.shouldBeTrue()
  }

  @Test fun testCache() {
    val scope = DisposableScope()
    var calls = 0
    scope.cache(0) { calls++ }
    scope.cache(0) { calls++ }
    scope.cache(1) { calls++ }
    calls shouldBe 2
  }

  @Test fun testDispose() {
    val scope = DisposableScope()
    scope.isDisposed.shouldBeFalse()
    scope.dispose()
    scope.isDisposed.shouldBeTrue()
  }

  @Test fun testDisposeWith() {
    val scope = DisposableScope()
    var called = false
    ScopeDisposable { called = true }.disposeWith(scope)
    called.shouldBeFalse()
    scope.dispose()
    called.shouldBeTrue()
  }

  @Test fun testDisposeWithOnDisposeOnDisposedScope() {
    val scope = DisposableScope()
    var called = false
    scope.dispose()
    ScopeDisposable { called = true }.disposeWith(scope)
    called.shouldBeTrue()
  }

  @Test fun testDoesNotDisposeIfReturnDisposableWasDisposed() {
    val scope = DisposableScope()
    var called = false
    val disposable = ScopeDisposable { called = true }.disposeWith(scope)
    disposable.dispose()
    called.shouldBeFalse()
    scope.dispose()
    called.shouldBeFalse()
  }

  @Test fun testInvokeOnDispose() {
    val scope = DisposableScope()
    var called = false
    scope.invokeOnDispose { called = true }
    called.shouldBeFalse()
    scope.dispose()
    called.shouldBeTrue()
  }
}
