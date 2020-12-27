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

@file:Suppress("NOTHING_TO_INLINE")

package com.ivianuu.injekt.android.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.GivenSetElement
import com.ivianuu.injekt.component.AppComponent
import com.ivianuu.injekt.component.Component
import com.ivianuu.injekt.component.componentElement
import com.ivianuu.injekt.component.element
import com.ivianuu.injekt.component.get
import kotlin.reflect.KClass

inline fun <reified T : ListenableWorker> worker():
        @GivenSetElement (@Given (@Given WorkerComponent) -> T) -> WorkerBinding = { T::class to it }

typealias WorkerComponent = Component

@GivenSetElement fun workerComponentFactory(
    @Given parent: AppComponent,
    @Given builderFactory: () -> Component.Builder<WorkerComponent>,
) = componentElement<AppComponent, (WorkerContext, WorkerParameters) -> WorkerComponent> {
        context, params ->
    builderFactory()
        .dependency(parent)
        .element(context)
        .element(params)
        .build()
}

typealias WorkerContext = Context

@Given val @Given WorkerComponent.workerContext: WorkerContext get() = get()

@Given val @Given WorkerComponent.workerParameters: WorkerParameters get() = get()

typealias WorkerBinding =
        Pair<KClass<out ListenableWorker>, (@Given WorkerComponent) -> ListenableWorker>

@Given inline val @Given WorkerComponent.appComponent: AppComponent
    get() = get()

@Given inline val @Given AppComponent.workerComponentFactory:
            (WorkerContext, WorkerParameters) -> WorkerComponent get() = get()

@Given class InjektWorkerFactory(
    @Given workersFactory: () -> Set<WorkerBinding>,
    @Given private val workerComponentFactory: (WorkerContext, WorkerParameters) -> WorkerComponent,
) : @Given WorkerFactory() {
    private val workers by lazy { workersFactory().toMap() }
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? {
        val workerFactory = workers[Class.forName(workerClassName).kotlin] ?: return null
        val component = workerComponentFactory(appContext, workerParameters)
        return workerFactory(component)
    }
}
