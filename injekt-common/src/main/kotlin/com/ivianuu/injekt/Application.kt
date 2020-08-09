/*
 * Copyright 2020 Manuel Wrage
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

package com.ivianuu.injekt

typealias ApplicationStorage = Storage

object ApplicationModule {
    @Given
    val applicationStorage: ApplicationStorage = Storage()
}

@Scoping
object ApplicationScoped {
    @Reader
    inline operator fun <T> invoke(
        key: Any,
        init: () -> T
    ) = given<ApplicationStorage>().scope(key, init)
}
