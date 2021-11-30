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

package com.ivianuu.injekt.coroutines

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.Tag
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

typealias DefaultContext = @DefaultContextTag CoroutineContext

@Tag annotation class DefaultContextTag {
  companion object {
    @Provide inline val context: DefaultContext
      get() = Dispatchers.Default
  }
}

typealias MainContext = @MainContextTag CoroutineContext

@Tag annotation class MainContextTag {
  companion object {
    @Provide inline val context: MainContext
      get() = Dispatchers.Main
  }
}

typealias ImmediateMainContext = @ImmediateMainContextTag CoroutineContext

@Tag annotation class ImmediateMainContextTag {
  companion object {
    @Provide inline val context: @ImmediateMainContextTag CoroutineContext
      get() = Dispatchers.Main.immediate
  }
}

typealias IOContext = @IOContextTag CoroutineContext

@Tag annotation class IOContextTag

expect object IOInjectables {
  @Provide val context: IOContext
}
