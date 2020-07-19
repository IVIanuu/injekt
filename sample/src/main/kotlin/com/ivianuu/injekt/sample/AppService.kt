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

package com.ivianuu.injekt.sample

import com.ivianuu.injekt.ApplicationComponent
import com.ivianuu.injekt.Distinct
import com.ivianuu.injekt.Effect
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.SetElements
import com.ivianuu.injekt.given
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Distinct
typealias AppServices = Set<suspend () -> Unit>

@Effect
annotation class BindAppService {
    companion object {
        @SetElements(ApplicationComponent::class)
        @Reader
        inline operator fun <reified T : suspend () -> Unit> invoke(): AppServices = setOf(
            given<T>()
        )
    }
}

@Reader
fun startAppServices() {
    println("app service init")
    given<AppServices>().forEach { service ->
        GlobalScope.launch { service() }
    }
}
