/*
 * Copyright 2018 Manuel Wrage
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

package com.ivianuu.injekt.sample.dummy

import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.android.ApplicationScope
import com.ivianuu.injekt.annotations.Factory
import com.ivianuu.injekt.annotations.Name
import com.ivianuu.injekt.annotations.Raw
import com.ivianuu.injekt.annotations.Reusable
import com.ivianuu.injekt.annotations.Single

@Single(ApplicationScope::class)
class DummyDep

@Reusable
class DummyDep2

@Single
class DummyDep3(
    @Name("name") val dummyDep: DummyDep,
    val dummyDep2: DummyDep2
)

@Factory
class DummyDep4(
    @Raw val rawProvider: Provider<String>,
    @Name("name") @Raw val namedRawLazy: Lazy<Int>
)