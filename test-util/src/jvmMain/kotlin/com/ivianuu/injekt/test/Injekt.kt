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

package com.ivianuu.injekt.test

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.Spread
import com.ivianuu.injekt.Tag
import com.ivianuu.injekt.common.Component
import com.ivianuu.injekt.common.ComponentObserver
import com.ivianuu.injekt.common.Disposable
import com.ivianuu.injekt.common.TypeKey

class Foo

class Bar(val foo: Foo)

class Baz(val bar: Bar, val foo: Foo)

interface Command

class CommandA : Command

class CommandB : Command

@Tag annotation class Tag1

@Tag annotation class Tag2

@Tag annotation class TypedTag<T>

class TestDisposable : Disposable {
  var disposeCalls = 0

  override fun dispose() {
    disposeCalls++
  }
}

class TestComponentObserver<C : Component> : ComponentObserver<C> {
  var initCalls = 0
  var disposeCalls = 0

  override fun init() {
    initCalls++
  }

  override fun dispose() {
    disposeCalls++
  }
}

interface Scope

object AppScope : Scope

@Tag annotation class FakeScoped<S : Scope> {
  companion object {
    inline fun <T : @FakeScoped<S> U, U : Any, S : Scope> scopedValue(
      factory: () -> T,
      scope: S,
      key: TypeKey<U>
    ): U = factory()
  }
}