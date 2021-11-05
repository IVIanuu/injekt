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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Tag annotation class DefaultDispatcherTag {
  companion object {
    @Provide inline val dispatcher: DefaultDispatcher
      get() = Dispatchers.Default
  }
}
typealias DefaultDispatcher = @DefaultDispatcherTag CoroutineDispatcher

@Tag annotation class MainDispatcherTag {
  companion object {
    @Provide inline val dispatcher: MainDispatcher
      get() = Dispatchers.Main
  }
}
typealias MainDispatcher = @MainDispatcherTag CoroutineDispatcher

@Tag annotation class ImmediateMainDispatcherTag {
  companion object {
    @Provide inline val dispatcher: @ImmediateMainDispatcherTag CoroutineDispatcher
      get() = Dispatchers.Main.immediate
  }
}
typealias ImmediateMainDispatcher = @ImmediateMainDispatcherTag CoroutineDispatcher

@Tag annotation class IODispatcherTag
typealias IODispatcher = @IODispatcherTag CoroutineDispatcher

expect object IOInjectables {
  @Provide val dispatcher: IODispatcher
}
