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
import com.ivianuu.injekt.common.*
import io.kotest.matchers.*
import io.kotest.matchers.booleans.*
import io.kotest.matchers.types.*
import org.junit.*

class ScopeTest {
  @Test fun testGetElement() {
    @Provide val element: @ScopeElement<TestScope1> String = "value"
    val scope = inject<TestScope1>()
    scope.element<String>() shouldBe "value"
  }

  @Test fun testScopeInitializer() {
    var called = false

    @Provide fun initializer(scope: TestScope1): ScopeInitializer<TestScope1> = {
      called = true
    }

    var otherCalled = false

    @Provide fun otherInitializer(): ScopeInitializer<TestScope2> = {
      otherCalled = true
    }

    val scope = inject<TestScope1>()
    called shouldBe true
    otherCalled shouldBe false
  }

  @Test fun testChildScopeModule() {
    @Provide val childScopeModule = ChildScopeModule1<TestScope1, String, TestScope2>()
    val parentScope = inject<TestScope1>()
    val childScope = parentScope.element<@ChildScopeFactory (String) -> TestScope2>()("42")
    childScope.element<String>() shouldBe "42"
  }

  @Test fun testGetSetScopedValue() {
    val scope = inject<TestScope1>()
    scope.getScopedValueOrNull<String>(0) shouldBe null
    scope.setScopedValue(0, "value")
    scope.getScopedValueOrNull<String>(0) shouldBe "value"
  }

  @Test fun testRemoveScopedValueDisposesValue() {
    val scope = inject<TestScope1>()
    var disposed = false
    scope.setScopedValue(0, ScopeDisposable { disposed = true })

    disposed.shouldBeFalse()
    scope.removeScopedValue(0)
    disposed.shouldBeTrue()
  }

  @Test fun testSetScopedValueDisposesOldValue() {
    val scope = inject<TestScope1>()
    var disposed = false
    scope.setScopedValue(0, ScopeDisposable { disposed = true })

    disposed.shouldBeFalse()
    scope.setScopedValue(0, 0)
    disposed.shouldBeTrue()
  }

  @Test fun testDisposeDisposesValues() {
    val scope = inject<TestScope1>()
    var disposed = false
    scope.setScopedValue(0, ScopeDisposable { disposed = true })

    disposed.shouldBeFalse()
    scope.dispose()
    disposed.shouldBeTrue()
  }

  @Test fun testGetOrCreateScopedValue() {
    val scope = inject<TestScope1>()
    var calls = 0
    scope.getOrCreateScopedValue(0) { calls++ }
    scope.getOrCreateScopedValue(0) { calls++ }
    scope.getOrCreateScopedValue(1) { calls++ }
    calls shouldBe 2
  }

  @Test fun testDispose() {
    val scope = inject<TestScope1>()
    scope.isDisposed.shouldBeFalse()
    scope.dispose()
    scope.isDisposed.shouldBeTrue()
  }

  @Test fun testInvokeOnDispose() {
    val scope = inject<TestScope1>()
    var called = false
    scope.invokeOnDispose { called = true }
    called.shouldBeFalse()
    scope.dispose()
    called.shouldBeTrue()
  }

  @Test fun testInvokeOnDisposeOnDisposedScope() {
    val scope = inject<TestScope1>()
    var called = false
    scope.dispose()
    scope.invokeOnDispose { called = true }
    called.shouldBeTrue()
  }

  @Test fun testDoesNotInvokeOnDisposeIfReturnDisposableWasDisposed() {
    val scope = inject<TestScope1>()
    var called = false
    val disposable = scope.invokeOnDispose { called = true }
    disposable.dispose()
    called.shouldBeFalse()
    scope.dispose()
    called.shouldBeFalse()
  }

  @Test fun testTypeKey() {
    val scope = inject<TestScope1>()
    scope.typeKey shouldBe inject<TypeKey<TestScope1>>()
  }

  @Test fun testParentScope() {
    @Provide val childScopeModule = ChildScopeModule0<TestScope1, TestScope2>()
    @Provide val childScope2Module = ChildScopeModule0<TestScope2, TestScope3>()
    val parentScope = inject<TestScope1>()
    val childScope1 = parentScope.element<@ChildScopeFactory () -> TestScope2>()
      .invoke()
    val childScope2 = childScope1.element<@ChildScopeFactory () -> TestScope3>()
      .invoke()
    childScope1.parent shouldBeSameInstanceAs parentScope
    childScope2.parent shouldBeSameInstanceAs childScope1
  }

  @Test fun testChildReturnsParentElement() {
    @Provide val parentElement: @ScopeElement<TestScope1> String = "value"
    @Provide val childScopeModule = ChildScopeModule0<TestScope1, TestScope2>()
    val parentScope = inject<TestScope1>()
    val childScope = parentScope.element<@ChildScopeFactory () -> TestScope2>()
      .invoke()
    childScope.element<String>() shouldBe "value"
  }

  @Test fun testDisposingParentDisposesChild() {
    @Provide val childScopeModule = ChildScopeModule0<TestScope1, TestScope2>()
    val parentScope = inject<TestScope1>()
    val childScope = parentScope.element<@ChildScopeFactory () -> TestScope2>()
      .invoke()
    childScope.isDisposed.shouldBeFalse()
    parentScope.dispose()
    childScope.isDisposed.shouldBeTrue()
  }
}
