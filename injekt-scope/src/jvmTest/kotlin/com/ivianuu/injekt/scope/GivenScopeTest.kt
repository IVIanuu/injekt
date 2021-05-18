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

@Providers("com.ivianuu.injekt.common.*")
class GivenScopeTest {
  @Test fun testGetElement() {
    @Provide val element: @InstallElement<TestGivenScope1> String = "value"
    val scope = inject<TestGivenScope1>()
    scope.element<String>() shouldBe "value"
  }

  @Test fun testGivenScopeInitializer() {
    var called = false

    @Provide
    fun initializer(givenScope: TestGivenScope1): GivenScopeInitializer<TestGivenScope1> = {
      called = true
    }

    var otherCalled = false

    @Provide fun otherInitializer(): GivenScopeInitializer<TestGivenScope2> = {
      otherCalled = true
    }

    val scope = inject<TestGivenScope1>()
    called shouldBe true
    otherCalled shouldBe false
  }

  @Test fun testChildGivenScopeModule() {
    @Provide val childScopeModule = ChildScopeModule1<TestGivenScope1, String, TestGivenScope2>()
    val parentScope = inject<TestGivenScope1>()
    val childScope = parentScope.element<@ChildScopeFactory (String) -> TestGivenScope2>()("42")
    childScope.element<String>() shouldBe "42"
  }

  @Test fun testGetSetScopedValue() {
    val scope = inject<TestGivenScope1>()
    scope.getScopedValueOrNull<String>(0) shouldBe null
    scope.setScopedValue(0, "value")
    scope.getScopedValueOrNull<String>(0) shouldBe "value"
  }

  @Test fun testRemoveScopedValueDisposesValue() {
    val scope = inject<TestGivenScope1>()
    var disposed = false
    scope.setScopedValue(0, GivenScopeDisposable { disposed = true })

    disposed.shouldBeFalse()
    scope.removeScopedValue(0)
    disposed.shouldBeTrue()
  }

  @Test fun testSetScopedValueDisposesOldValue() {
    val scope = inject<TestGivenScope1>()
    var disposed = false
    scope.setScopedValue(0, GivenScopeDisposable { disposed = true })

    disposed.shouldBeFalse()
    scope.setScopedValue(0, 0)
    disposed.shouldBeTrue()
  }

  @Test fun testDisposeDisposesValues() {
    val scope = inject<TestGivenScope1>()
    var disposed = false
    scope.setScopedValue(0, GivenScopeDisposable { disposed = true })

    disposed.shouldBeFalse()
    scope.dispose()
    disposed.shouldBeTrue()
  }

  @Test fun testGetOrCreateScopedValue() {
    val scope = inject<TestGivenScope1>()
    var calls = 0
    scope.getOrCreateScopedValue(0) { calls++ }
    scope.getOrCreateScopedValue(0) { calls++ }
    scope.getOrCreateScopedValue(1) { calls++ }
    calls shouldBe 2
  }

  @Test fun testDispose() {
    val scope = inject<TestGivenScope1>()
    scope.isDisposed.shouldBeFalse()
    scope.dispose()
    scope.isDisposed.shouldBeTrue()
  }

  @Test fun testInvokeOnDispose() {
    val scope = inject<TestGivenScope1>()
    var called = false
    scope.invokeOnDispose { called = true }
    called.shouldBeFalse()
    scope.dispose()
    called.shouldBeTrue()
  }

  @Test fun testInvokeOnDisposeOnDisposedScope() {
    val scope = inject<TestGivenScope1>()
    var called = false
    scope.dispose()
    scope.invokeOnDispose { called = true }
    called.shouldBeTrue()
  }

  @Test fun testDoesNotInvokeOnDisposeIfReturnDisposableWasDisposed() {
    val scope = inject<TestGivenScope1>()
    var called = false
    val disposable = scope.invokeOnDispose { called = true }
    disposable.dispose()
    called.shouldBeFalse()
    scope.dispose()
    called.shouldBeFalse()
  }

  @Test fun testTypeKey() {
    val scope = inject<TestGivenScope1>()
    scope.typeKey shouldBe typeKeyOf<TestGivenScope1>()
  }

  @Test fun testParentScope() {
    @Provide val childScopeModule = ChildScopeModule0<TestGivenScope1, TestGivenScope2>()
    @Provide val childScope2Module = ChildScopeModule0<TestGivenScope2, TestGivenScope3>()
    val parentScope = inject<TestGivenScope1>()
    val childScope1 = parentScope.element<@ChildScopeFactory () -> TestGivenScope2>()
      .invoke()
    val childScope2 = childScope1.element<@ChildScopeFactory () -> TestGivenScope3>()
      .invoke()
    childScope1.parent shouldBeSameInstanceAs parentScope
    childScope2.parent shouldBeSameInstanceAs childScope1
  }

  @Test fun testChildReturnsParentElement() {
    @Provide val parentElement: @InstallElement<TestGivenScope1> String = "value"
    @Provide val childScopeModule = ChildScopeModule0<TestGivenScope1, TestGivenScope2>()
    val parentScope = inject<TestGivenScope1>()
    val childScope = parentScope.element<@ChildScopeFactory () -> TestGivenScope2>()
      .invoke()
    childScope.element<String>() shouldBe "value"
  }

  @Test fun testDisposingParentDisposesChild() {
    @Provide val childScopeModule = ChildScopeModule0<TestGivenScope1, TestGivenScope2>()
    val parentScope = inject<TestGivenScope1>()
    val childScope = parentScope.element<@ChildScopeFactory () -> TestGivenScope2>()
      .invoke()
    childScope.isDisposed.shouldBeFalse()
    parentScope.dispose()
    childScope.isDisposed.shouldBeTrue()
  }
}
