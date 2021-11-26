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

class ScopeTest {
  @Test fun testScope() {
    val scope = Scope<Any>()
    scope.scope { "a" } shouldBe "a"
    scope.scope { "b" } shouldBe "a"
  }

  @Test fun testDispose() {
    val scope = Scope<Any>()
    var disposeCalls = 0
    scope.scope {
      Disposable {
        disposeCalls++
      }
    }
    disposeCalls shouldBe 0
    scope.dispose()
    disposeCalls shouldBe 1
    scope.dispose()
    disposeCalls shouldBe 1
  }

  @Test fun testScoped() {
    var callCount = 0

    class Foo

    @Provide fun scopedFoo(): @Scoped<AppComponent> Foo {
      callCount++
      return Foo()
    }

    @Provide val scope = Scope<AppComponent>()
    callCount shouldBe 0
    val a = inject<Foo>()
    callCount shouldBe 1
    val b = inject<Foo>()
    callCount shouldBe 1
    a shouldBeSameInstanceAs b
  }

  @Test fun testEager() {
    var callCount = 0

    class Foo

    @Provide fun eagerFoo(): @Eager<AppComponent> Foo {
      callCount++
      return Foo()
    }

    @Provide val component = inject<Component<AppComponent>>()
    @Provide val scope = component.element<Scope<AppComponent>>()
    callCount shouldBe 1
    val a = inject<Foo>()
    callCount shouldBe 1
    val b = inject<Foo>()
    callCount shouldBe 1
    a shouldBeSameInstanceAs b
  }
}
