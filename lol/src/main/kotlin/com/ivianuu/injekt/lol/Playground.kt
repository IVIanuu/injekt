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

package com.ivianuu.injekt.lol

import com.ivianuu.injekt.Module
import com.ivianuu.injekt.composition.BindingAdapter
import com.ivianuu.injekt.composition.CompositionComponent
import com.ivianuu.injekt.composition.CompositionFactory
import com.ivianuu.injekt.composition.compositionFactoryOf
import com.ivianuu.injekt.composition.initializeCompositions
import com.ivianuu.injekt.composition.reader
import com.ivianuu.injekt.create
import com.ivianuu.injekt.get
import com.ivianuu.injekt.map
import com.ivianuu.injekt.scoped
import kotlin.reflect.KClass

@CompositionComponent
interface TestCompositionComponent

@CompositionFactory
fun factory(): TestCompositionComponent {
    map<KClass<out AppService>, AppService>()
    return create()
}

interface AppService

@BindingAdapter(TestCompositionComponent::class)
annotation class BindAppService {
    companion object {
        @Module
        inline operator fun <reified T : AppService> invoke() {
            scoped<T>()
            map<KClass<out AppService>, AppService> {
                put<T>(T::class)
            }
        }
    }
}

@BindAppService
class MyAppServiceA : AppService

@BindAppService
class MyAppServiceB : AppService

fun invoke() {
    initializeCompositions()
    val component =
        compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
    val appServices = component.reader { get<Map<KClass<out AppService>, AppService>>() }
    println("app services " + appServices)
}
