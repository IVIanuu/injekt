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
import com.ivianuu.injekt.component.ApplicationScoped
import com.ivianuu.injekt.component.Component
import com.ivianuu.injekt.component.ComponentKey
import com.ivianuu.injekt.component.componentElement
import com.ivianuu.injekt.component.get
import com.ivianuu.injekt.component.getDependency
import com.ivianuu.injekt.given
import kotlin.reflect.KClass

inline fun <reified T : ListenableWorker> worker(
    noinline workerFactory: @Given Component<WorkerScoped>.() -> T = given,
): WorkerBinding = T::class to workerFactory

@Given object WorkerScoped : Component.Name

private val WorkerComponentFactoryKey =
    ComponentKey<(Context, WorkerParameters) -> Component<WorkerScoped>>()

@GivenSetElement fun workerComponentFactoryKey(
    builderFactory: () -> Component.Builder<WorkerScoped> = given,
) = componentElement(ApplicationScoped, WorkerComponentFactoryKey) { context, params ->
    builderFactory()
        .element(WorkerContextKey, context)
        .element(WorkerParametersKey, params)
        .build()
}

@Given inline val @Given Component<WorkerScoped>.applicationComponent: Component<ApplicationScoped>
    get() = getDependency(ApplicationScoped)

private val WorkerContextKey = ComponentKey<WorkerContext>()
typealias WorkerContext = Context

@Given val @Given Component<WorkerScoped>.workerContext: WorkerContext get() = this[WorkerContextKey]

private val WorkerParametersKey = ComponentKey<WorkerParameters>()
@Given val @Given Component<WorkerScoped>.workerParameters: WorkerParameters
    get() = this[WorkerParametersKey]

typealias WorkerBinding =
        Pair<KClass<out ListenableWorker>, @Given Component<WorkerScoped>.() -> ListenableWorker>

@Given class InjektWorkerFactory(
    workersFactory: () -> Set<WorkerBinding> = given,
    private val workerComponentFactory: (Context, WorkerParameters) -> Component<WorkerScoped> = given,
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
