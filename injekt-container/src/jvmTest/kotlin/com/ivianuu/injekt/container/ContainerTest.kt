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

package com.ivianuu.injekt.container

import com.ivianuu.injekt.*
import io.kotest.matchers.*
import io.kotest.matchers.booleans.*
import org.junit.*

class ContainerTest {
  @Test fun testGetElement() {
    @Provide val element: @ContainerElement<TestScope1> String = "value"
    val container = inject<Container<TestScope1>>()
    container.element<String>() shouldBe "value"
  }

  @Test fun testContainerObserver() {
    var initCalls = 0
    var disposeCalls = 0
    @Provide val observer = object : ContainerObserver<TestScope1> {
      override fun onInit() {
        initCalls++
      }

      override fun onDispose() {
        disposeCalls++
      }
    }

    val container = inject<Container<TestScope1>>()
    
    initCalls shouldBe 1
    disposeCalls shouldBe 0

    container.dispose()

    initCalls shouldBe 1
    disposeCalls shouldBe 1
  }

  @Test fun testChildContainerModule() {
    @Provide val childContainerModule = ChildContainerModule1<TestScope1, String, TestScope2>()
    val parentContainer = inject<Container<TestScope1>>()
    val childContainer = parentContainer.element<@ChildContainerFactory (String) -> Container<TestScope2>>()("42")
    childContainer.element<String>() shouldBe "42"
  }

  @Test fun testChildReturnsParentElement() {
    @Provide val parentElement: @ContainerElement<TestScope1> String = "value"
    @Provide val childContainerModule = ChildContainerModule0<TestScope1, TestScope2>()
    val parentContainer = inject<Container<TestScope1>>()
    val childContainer = parentContainer.element<@ChildContainerFactory () -> Container<TestScope2>>()
      .invoke()
    childContainer.element<String>() shouldBe "value"
  }

  @Test fun testDisposingParentDisposesChild() {
    @Provide val childContainerModule = ChildContainerModule0<TestScope1, TestScope2>()
    val parentContainer = inject<Container<TestScope1>>()
    val childContainer = parentContainer.element<@ChildContainerFactory () -> Container<TestScope2>>()
      .invoke()
    val childScope = childContainer.element<NamedScope<TestScope2>>()
    childScope.isDisposed.shouldBeFalse()
    parentContainer.dispose()
    childScope.isDisposed.shouldBeTrue()
  }
}

abstract class TestScope1 private constructor()
abstract class TestScope2 private constructor()
abstract class TestScope3 private constructor()
