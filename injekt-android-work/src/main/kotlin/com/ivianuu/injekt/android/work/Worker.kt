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
import com.ivianuu.injekt.GivenMap
import com.ivianuu.injekt.given
import kotlin.reflect.KClass

data class WorkerContext(
    @Given val context: Context,
    @Given val workerParameters: WorkerParameters,
)

inline fun <reified T : ListenableWorker> workerMapOf(
    noinline workerFactory: @Given WorkerContext.() -> T = given,
): Workers = mapOf(T::class to workerFactory)

typealias Workers = Map<KClass<out ListenableWorker>, @Given WorkerContext.() -> ListenableWorker>

@GivenMap inline fun defaultWorkers(): Workers = emptyMap()

@Given class InjektWorkerFactory(workersFactory: () -> Workers = given) : @Given WorkerFactory() {
    private val workers by lazy(workersFactory)
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = workers[Class.forName(workerClassName).kotlin]
        ?.invoke(WorkerContext(appContext, workerParameters))
}
