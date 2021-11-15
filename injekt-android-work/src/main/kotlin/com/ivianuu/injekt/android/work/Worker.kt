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
import com.ivianuu.injekt.Spread
import com.ivianuu.injekt.Tag
import com.ivianuu.injekt.common.Incremental
import kotlin.reflect.KClass

/**
 * Installs the injectable [ListenableWorker] in the [InjektWorkerFactory]
 *
 * Example:
 * ```
 * @Provide
 * @InjektWorker
 * class MyWorker(
 *   context: Context,
 *   parameters: WorkerParameters
 * ) : CoroutineWorker(context, parameters)
 * ```
 */
@Tag annotation class InjektWorker {
  companion object {
    @Provide inline fun <@Spread T : @InjektWorker S, S : ListenableWorker> workerFactory(
      noinline factory: (Context, WorkerParameters) -> T,
      workerClass: KClass<S>
    ): Pair<String, SingleWorkerFactory> = workerClass.java.name to factory

    @Provide val defaultWorkers: List<Pair<String, SingleWorkerFactory>> get() = emptyList()
  }
}

/**
 * Factory which is able to create [ListenableWorker]s installed via [InjektWorker]
 */
@Provide class InjektWorkerFactory(
  private val workers: @Incremental Map<String, SingleWorkerFactory>
) : WorkerFactory() {
  override fun createWorker(
    appContext: Context,
    workerClassName: String,
    workerParameters: WorkerParameters,
  ): ListenableWorker? = workers[workerClassName]?.invoke(appContext, workerParameters)
}

@Tag private annotation class SingleWorkerFactoryTag
private typealias SingleWorkerFactory = @SingleWorkerFactoryTag (Context, WorkerParameters) -> ListenableWorker
