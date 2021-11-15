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

package com.ivianuu.injekt.samples.android

import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.Tag
import com.ivianuu.injekt.common.Incremental
import com.ivianuu.injekt.common.SourceKey
@Tag annotation class StackedKeyTag
typealias StackedKey = @Incremental @StackedKeyTag String

@Provide fun stackedKey(sourceKey: SourceKey): @StackedKeyTag String = sourceKey.value

@Provide
val StackedKeyIncrementalFactory = Incremental.Factory<StackedKey, @StackedKeyTag String> { combined, elements ->
  buildString {
    combined?.forEach { append(it) }
    elements?.forEach { append(it) }
  }
}
