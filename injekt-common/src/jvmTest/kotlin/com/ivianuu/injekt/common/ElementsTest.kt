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

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.inject
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.Test

class ElementsTest {
  @Test fun testElements() {
    class MyScope

    @Provide val int: @Element<MyScope> Int = 42
    @Provide val string: @Element<MyScope> String = "42"

    val elements = inject<Elements<MyScope>>()

    elements<Int>() shouldBe 42
    elements<String>() shouldBe "42"
  }

  @Test fun testElementsCanAccessParentElements() {
    class ParentScope
    class ChildScope

    @Provide @Element<ParentScope> data class ChildElement(
      val childElements: Elements<ChildScope>
    )

    @Provide val int: @Element<ParentScope> Int = 42

    val parentElements = inject<Elements<ParentScope>>()
    val childElements = parentElements<ChildElement>().childElements

    childElements<Int>() shouldBe 42
  }

  @Test fun testElementsCanBeAccessedInElement() {
    class MyScope

    @Provide fun element(elements: Elements<MyScope>):
        @Element<MyScope> () -> Elements<MyScope> = { elements }

    val elements = inject<Elements<MyScope>>()

    elements<() -> Elements<MyScope>>()() shouldBeSameInstanceAs elements
  }

  @Test fun testEager() {
    var callCount = 0

    class Foo

    @Provide fun eagerFoo(): @Eager<TestScope> Foo {
      callCount++
      return Foo()
    }

    @Provide val scope = Scope<TestScope>()
    @Provide val elements = inject<Elements<TestScope>>()
    callCount shouldBe 1
    val a = inject<Foo>()
    callCount shouldBe 1
    val b = inject<Foo>()
    callCount shouldBe 1
    a shouldBeSameInstanceAs b
  }
}
