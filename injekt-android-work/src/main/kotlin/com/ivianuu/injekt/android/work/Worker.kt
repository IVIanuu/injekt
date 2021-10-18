/*
 * Copyright 2021 Manuel Wrage
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
import com.ivianuu.injekt.Provide
import kotlin.reflect.KClass

/**
 * Installs the injectable [ListenableWorker] in the [InjektWorkerFactory]
 *
 * Example:
 * ```
 * @Provide
 * @InjektWorker
 * class MyWorker(
 *   context: AppContext,
 *   parameters: WorkerParameters
 * ) : CoroutineWorker(context, parameters)
 * ```
 */
class WorkerModule<T : ListenableWorker> {
  @Provide fun workerFactory(
    factory: (@Provide WorkerParameters) -> T,
    workerClass: KClass<T>
  ): Pair<String, SingleWorkerFactory> = workerClass.java.name to factory
}

/**
 * Factory which is able to create [ListenableWorker]s installed via [WorkerModule]
 */
@Provide class InjektWorkerFactory(
  private val workers: Map<String, SingleWorkerFactory>
) : WorkerFactory() {
  override fun createWorker(
    appContext: Context,
    workerClassName: String,
    workerParameters: WorkerParameters,
  ): ListenableWorker? = workers[workerClassName]?.invoke(workerParameters)
}

typealias SingleWorkerFactory = (WorkerParameters) -> ListenableWorker
