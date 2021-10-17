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

import com.ivianuu.injekt.common.Component
import com.ivianuu.injekt.common.ComponentObserver
import com.ivianuu.injekt.common.Disposable

class Foo

class Bar(val foo: Foo)

class Baz(val bar: Bar, val foo: Foo)

interface Command

class CommandA : Command

class CommandB : Command

class TestDisposable : Disposable {
  var disposeCalls = 0
  override fun dispose() {
    disposeCalls++
  }
}

class TestComponentObserver<C : @Component Any> : ComponentObserver<C> {
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
