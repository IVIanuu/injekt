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

package kotlinx.coroutines

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.coroutines.ComponentScope
import com.ivianuu.injekt.coroutines.DefaultDispatcher

@Provide inline fun coroutineScope(scope: ComponentScope<*>): CoroutineScope =
  scope

@Provide inline fun dispatcher(dispatcher: DefaultDispatcher): CoroutineDispatcher =
  dispatcher
