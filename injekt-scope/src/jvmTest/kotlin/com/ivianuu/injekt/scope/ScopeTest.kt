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

package com.ivianuu.injekt.scope

import com.ivianuu.injekt.*
import io.kotest.matchers.*
import io.kotest.matchers.booleans.*
import org.junit.*

@OptIn(InternalScopeApi::class)
class ScopeTest {
  @Test fun testGetSet() {
    val scope = DisposableScope()
    scope.getScopedValueOrNull<String>(0) shouldBe null
    scope.setScopedValue(0, "value")
    scope.getScopedValueOrNull<String>(0) shouldBe "value"
  }

  @Test fun testRemoveDisposesValue() {
    val scope = DisposableScope()
    var disposed = false
    scope.setScopedValue(0, Disposable { disposed = true })

    disposed.shouldBeFalse()
    scope.removeScopedValue(0)
    disposed.shouldBeTrue()
  }

  @Test fun testSetDisposesOldValue() {
    val scope = DisposableScope()
    var disposed = false
    scope.setScopedValue(0, Disposable { disposed = true })

    disposed.shouldBeFalse()
    scope.setScopedValue(0, 0)
    disposed.shouldBeTrue()
  }

  @Test fun testDisposeDisposesValues() {
    val scope = DisposableScope()
    var disposed = false
    scope.setScopedValue(0, Disposable { disposed = true })

    disposed.shouldBeFalse()
    scope.dispose()
    disposed.shouldBeTrue()
  }

  @Test fun testScoped() {
    @Provide val scope = DisposableScope()
    var calls = 0
    scoped(0) { calls++ }
    scoped(0) { calls++ }
    scoped(1) { calls++ }
    calls shouldBe 2
  }

  @Test fun testDispose() {
    val scope = DisposableScope()
    scope.isDisposed.shouldBeFalse()
    scope.dispose()
    scope.isDisposed.shouldBeTrue()
  }

  @Test fun testDisposableBind() {
    @Provide val scope = DisposableScope()
    var called = false
    Disposable { called = true }.bind()
    called.shouldBeFalse()
    scope.dispose()
    called.shouldBeTrue()
  }

  @Test fun testDisposerAsDisposable() {
    var called = false
    @Provide val disposer = Disposer<Unit> { called = true }
    val disposable = Unit.asDisposable()
    called.shouldBeFalse()
    disposable.dispose()
    called.shouldBeTrue()
  }

  @Test fun testDisposerBind() {
    var called = false
    @Provide val disposer = Disposer<Unit> { called = true }
    withScope {
      Unit.bind()
      called.shouldBeFalse()
    }
    called.shouldBeTrue()
  }

  @Test fun testDisposeWithOnDisposeOnDisposedScope() {
    @Provide val scope = DisposableScope()
    var called = false
    scope.dispose()
    Disposable { called = true }.bind()
    called.shouldBeTrue()
  }

  @Test fun testInvokeOnDispose() {
    @Provide val scope = DisposableScope()
    var called = false
    invokeOnDispose { called = true }
    called.shouldBeFalse()
    scope.dispose()
    called.shouldBeTrue()
  }

  @Test fun testWithScope() {
    var open = true
    withScope {
      invokeOnDispose { open = false }
      open.shouldBeTrue()
    }
    open.shouldBeFalse()
  }
}
