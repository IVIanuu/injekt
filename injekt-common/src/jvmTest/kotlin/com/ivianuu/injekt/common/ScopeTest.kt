/*
 * Copyright 2020 Manuel Wrage
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

package com.ivianuu.injekt.common

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.Test

class ScopeTest {

    @Test
    fun testGetSet() {
        val scope = Scope()
        scope.get<String>(0) shouldBe null
        scope.set(0, "value")
        scope.get<String>(0) shouldBe "value"
    }

    @Test
    fun testRemoveDisposesValue() {
        val scope = Scope()
        var disposed = false
        scope.getOrCreate(0) {
            ScopeDisposable { disposed = true }
        }

        disposed.shouldBeFalse()
        scope.remove(0)
        disposed.shouldBeTrue()
    }

    @Test
    fun testSetDisposesOldValue() {
        val scope = Scope()
        var disposed = false
        scope.getOrCreate(0) {
            ScopeDisposable { disposed = true }
        }

        disposed.shouldBeFalse()
        scope.set(0, 0)
        disposed.shouldBeTrue()
    }

    @Test
    fun testDisposeDisposesValues() {
        val scope = Scope()
        var disposed = false
        scope.getOrCreate(0) {
            ScopeDisposable { disposed = true }
        }

        disposed.shouldBeFalse()
        scope.dispose()
        disposed.shouldBeTrue()
    }

    @Test
    fun testScope() {
        val scope = Scope()
        var calls = 0
        scope.getOrCreate(0) { calls++ }
        scope.getOrCreate(0) { calls++ }
        scope.getOrCreate(1) { calls++ }
        calls shouldBe 2
    }

    @Test
    fun testDispose() {
        val scope = Scope()
        scope.isDisposed.shouldBeFalse()
        scope.dispose()
        scope.isDisposed.shouldBeTrue()
    }

    @Test
    fun testInvokeOnDispose() {
        val scope = Scope()
        var called = false
        scope.invokeOnDispose { called = true }
        called.shouldBeFalse()
        scope.dispose()
        called.shouldBeTrue()
    }

    @Test
    fun testInvokeOnDisposeOnDisposedScope() {
        val scope = Scope()
        var called = false
        scope.dispose()
        scope.invokeOnDispose { called = true }
        called.shouldBeTrue()
    }

    @Test
    fun testDoesNotInvokeOnDisposeIfReturnDisposableWasDisposed() {
        val scope = Scope()
        var called = false
        val disposable = scope.invokeOnDispose { called = true }
        disposable.dispose()
        called.shouldBeFalse()
        scope.dispose()
        called.shouldBeFalse()
    }
}
