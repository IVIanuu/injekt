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

package com.ivianuu.injekt.android.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.MapEntries
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.alias
import kotlin.reflect.KClass

inline fun <reified T : ListenableWorker> worker(): @MapEntries ((Context, WorkerParameters) -> T) -> Workers =
    { factory -> mapOf(T::class to factory) }

typealias Workers = Map<KClass<out ListenableWorker>, (Context, WorkerParameters) -> ListenableWorker>

@Suppress("NOTHING_TO_INLINE")
@MapEntries inline fun defaultWorkers(): Workers = emptyMap()

@Module val InjektWorkerFactoryModule = alias<InjektWorkerFactory, WorkerFactory>()
@Binding class InjektWorkerFactory(workersFactory: () -> Workers) : WorkerFactory() {
    private val workers by lazy(workersFactory)
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? {
        return workers[Class.forName(workerClassName).kotlin]?.invoke(
            appContext,
            workerParameters
        )
    }
}
