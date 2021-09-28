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

import com.ivianuu.injekt.Provide
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.Test

@OptIn(InternalScopeApi::class)
class ScopeTest {
  @Test fun testGetSet() {
    val scope = scopeOf()
    scope.getScopedValueOrNull<String>(0) shouldBe null
    scope.setScopedValue(0, "value")
    scope.getScopedValueOrNull<String>(0) shouldBe "value"
  }

  @Test fun testRemoveDisposesValue() {
    val scope = scopeOf()
    var disposed = false
    scope.setScopedValue(0, Disposable { disposed = true })

    disposed.shouldBeFalse()
    scope.removeScopedValue(0)
    disposed.shouldBeTrue()
  }

  @Test fun testSetDisposesOldValue() {
    val scope = scopeOf()
    var disposed = false
    scope.setScopedValue(0, Disposable { disposed = true })

    disposed.shouldBeFalse()
    scope.setScopedValue(0, 0)
    disposed.shouldBeTrue()
  }

  @Test fun testDisposeDisposesValues() {
    val scope = scopeOf()
    var disposed = false
    scope.setScopedValue(0, Disposable { disposed = true })

    disposed.shouldBeFalse()
    scope.dispose()
    disposed.shouldBeTrue()
  }

  @Test fun testScoped() {
    @Provide val scope = scopeOf()
    var calls = 0
    scoped(0) { calls++ }
    scoped(0) { calls++ }
    scoped(1) { calls++ }
    calls shouldBe 2
  }

  @Test fun testScopedValueGetsDisposed() {
    @Provide val scope = scopeOf()
    var disposed = false
    scoped(0) { Disposable { disposed = true } }
    disposed.shouldBeFalse()
    scope.dispose()
    disposed.shouldBeTrue()
  }

  @Test fun testScopedValueScopeObserver() {
    @Provide val scope = scopeOf()
    var initialized = false
    var disposed = false
    scoped(0) {
      object : ScopeObserver {
        override fun init() {
          initialized = true
        }

        override fun dispose() {
          disposed = true
        }
      }
    }
    initialized.shouldBeTrue()
    disposed.shouldBeFalse()
    scope.dispose()
    initialized.shouldBeTrue()
    disposed.shouldBeTrue()
  }

  @Test fun testScopedWithArgs() {
    @Provide val scope = scopeOf()
    var calls = 0
    scoped(0, "a") { calls++ }
    scoped(0, "a") { calls++ }
    scoped(0, "b") { calls++ }
    scoped(1) { calls++ }
    calls shouldBe 3
  }

  @Test fun testScopedWithArgsValueGetsDisposed() {
    @Provide val scope = scopeOf()
    var disposeCalls = 0
    scoped(0, "a") { Disposable { disposeCalls++ } }
    disposeCalls shouldBe 0
    scoped(0, "b") { Disposable { disposeCalls++ } }
    disposeCalls shouldBe 1
    scope.dispose()
    disposeCalls shouldBe 2
  }

  @Test fun testScopedWithArgsScopeObserver() {
    @Provide val scope = scopeOf()
    var initCalls = 0
    var disposeCalls = 0
    scoped(0, "a") {
      object : ScopeObserver {
        override fun init() {
          initCalls++
        }

        override fun dispose() {
          disposeCalls++
        }
      }
    }
    initCalls shouldBe 1
    disposeCalls shouldBe 0
    scoped(0, "b") {
      object : ScopeObserver {
        override fun init() {
          initCalls++
        }

        override fun dispose() {
          disposeCalls++
        }
      }
    }
    initCalls shouldBe 2
    disposeCalls shouldBe 1
    scope.dispose()
    disposeCalls shouldBe 2
  }

  @Test fun testScopeDispose() {
    val scope = scopeOf()
    scope.isDisposed.shouldBeFalse()
    scope.dispose()
    scope.isDisposed.shouldBeTrue()
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
    @Provide val scope = scopeOf()
    var called = false
    scope.dispose()
    Disposable { called = true }.bind()
    called.shouldBeTrue()
  }

  @Test fun testOnDispose() {
    @Provide val scope = scopeOf()
    var called = false
    onDispose { called = true }
    called.shouldBeFalse()
    scope.dispose()
    called.shouldBeTrue()
  }

  @Test fun testWithScope() {
    var open = true
    withScope {
      onDispose { open = false }
      open.shouldBeTrue()
    }
    open.shouldBeFalse()
  }

  @Test fun testScopeObserver() {
    var initCalled = false
    var disposeCalled = false

    val observer = object : ScopeObserver {
      override fun init() {
        initCalled = true
      }

      override fun dispose() {
        disposeCalled = true
      }
    }

    @Provide val scope = scopeOf()
    initCalled.shouldBeFalse()
    disposeCalled.shouldBeFalse()

    scope.setScopedValue(Unit, observer)
    initCalled.shouldBeTrue()
    disposeCalled.shouldBeFalse()

    scope.removeScopedValue(Unit)
    initCalled.shouldBeTrue()
    disposeCalled.shouldBeTrue()
  }

  @Test fun testDisposable() {
    var called = false
    @Provide val disposer = Disposer<Unit> { called = true }
    withScope {
      Unit.bind()
      called.shouldBeFalse()
    }
    called.shouldBeTrue()
  }

  @Test fun testDisposerUse() {
    var called = false
    @Provide val disposer = Disposer<Unit> { called = true }
    Unit.use { called.shouldBeFalse() }
    called.shouldBeTrue()
  }

  @Test fun testDisposableDisposer() {
    @Provide val scope = scopeOf()
    var called = false
    Disposable { called = true }.bind()
    called.shouldBeFalse()
    scope.dispose()
    called.shouldBeTrue()
  }
}
