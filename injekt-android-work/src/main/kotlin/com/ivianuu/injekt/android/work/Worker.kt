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
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.GivenSetElement
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.android.AppContext
import com.ivianuu.injekt.component.AppComponent
import com.ivianuu.injekt.component.ChildComponentModule2
import com.ivianuu.injekt.component.Component
import kotlin.reflect.KClass

@Qualifier
annotation class WorkerBinding

@GivenSetElement
inline fun <@Given reified T : @WorkerBinding S, S : ListenableWorker> workerBindingImpl(
    @Given noinline provider: (@Given WorkerComponent) -> T
): WorkerElement = T::class to provider

typealias WorkerComponent = Component

@Module
val workerComponentModule =
    ChildComponentModule2<AppComponent, WorkerContext, WorkerParameters, WorkerComponent>()

typealias WorkerContext = Context

typealias WorkerElement =
        Pair<KClass<out ListenableWorker>, (@Given WorkerComponent) -> ListenableWorker>

@Given
val @Given AppContext.workManager: WorkManager
    get() = WorkManager.getInstance(this)

@Given
class InjektWorkerFactory(
    @Given workersFactory: () -> Set<WorkerElement>,
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
