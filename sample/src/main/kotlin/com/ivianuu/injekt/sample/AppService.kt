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
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.composition.BindingAdapter
import com.ivianuu.injekt.composition.BindingEffect
import com.ivianuu.injekt.get
import com.ivianuu.injekt.map
import com.ivianuu.injekt.scoped
import com.ivianuu.injekt.set
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

typealias AppService = suspend () -> Unit

@BindingEffect(ApplicationComponent::class)
annotation class BindAppService {
    companion object {
        @Module
        inline operator fun <reified T : AppService> invoke() {
            set<AppService> {
                add<T>()
            }
        }
    }
}

@Reader
fun startAppServices() {
    println("app service init")
    get<Set<AppService>>().forEach { service ->
        GlobalScope.launch { service() }
    }
}
