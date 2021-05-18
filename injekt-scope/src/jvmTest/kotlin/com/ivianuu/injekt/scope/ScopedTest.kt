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
import org.junit.*

@GivenImports("com.ivianuu.injekt.common.*")
class ScopedTest {
  @Qualifier private annotation class Element

  @Test fun testScoped() {
    var callCount = 0

    class Foo

    @Given fun scopedFoo(): @Scoped<TestGivenScope1> Foo {
      callCount++
      return Foo()
    }

    @Given fun fooElement(@Given foo: Foo): @InstallElement<TestGivenScope1> @Element Foo = foo
    val scope = summon<TestGivenScope1>()
    callCount shouldBe 0
    scope.element<@Element Foo>()
    callCount shouldBe 1
    scope.element<@Element Foo>()
    callCount shouldBe 1
  }
}