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

class ElementScopeTest {
  @Test fun testGetElement() {
    @Provide val element: @ScopeElement<TestScope1> String = "value"
    val scope = inject<TestScope1>()
    scope.element<String>() shouldBe "value"
  }

  @Test fun testScopeObserver() {
    var initCalls = 0
    var disposeCalls = 0
    @Provide val observer = object : ScopeObserver<TestScope1> {
      override fun onInit() {
        initCalls++
      }

      override fun onDispose() {
        disposeCalls++
      }
    }

    val scope = inject<TestScope1>()

    initCalls shouldBe 1
    disposeCalls shouldBe 0

    (scope as DisposableScope).dispose()

    initCalls shouldBe 1
    disposeCalls shouldBe 1
  }

  @Test fun testChildScopeModule() {
    @Provide val childScopeModule = ChildScopeModule1<TestScope1, String, TestScope2>()
    val parentScope = inject<TestScope1>()
    val childScope = parentScope.element<@ChildScopeFactory (String) -> TestScope2>()("42")
    childScope.element<String>() shouldBe "42"
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
    (parentScope as DisposableScope).dispose()
    childScope.isDisposed.shouldBeTrue()
  }
}

typealias TestScope1 = Scope
typealias TestScope2 = Scope
typealias TestScope3 = Scope
