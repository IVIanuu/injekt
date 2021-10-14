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

package com.ivianuu.injekt.android

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.Component
import com.ivianuu.injekt.common.Disposable
import com.ivianuu.injekt.common.EntryPoint
import com.ivianuu.injekt.common.Scoped

class TestDisposable<C : @Component Any> @Provide @Scoped<C> constructor() : Disposable {
  var disposed = false

  override fun dispose() {
    disposed = true
  }
}

@EntryPoint<Any> interface TestDisposableComponent<C : @Component Any> {
  val disposable: TestDisposable<C>
}
