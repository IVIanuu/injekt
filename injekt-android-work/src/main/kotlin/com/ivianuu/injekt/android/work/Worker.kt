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

import android.content.*
import androidx.work.*
import com.ivianuu.injekt.*
import com.ivianuu.injekt.ambient.*
import com.ivianuu.injekt.android.*
import kotlin.reflect.*

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
@Tag annotation class InjektWorker {
  companion object {
    @Provide inline fun <@Spread T : @InjektWorker S, S : ListenableWorker> workerFactory(
      noinline factory: (@Provide WorkerParameters) -> T,
      workerClass: KClass<S>
    ): Pair<String, SingleWorkerFactory> = workerClass.java.name to factory
  }
}

/**
 * Factory which is able to create [ListenableWorker]s installed via [InjektWorker]
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

/**
 * Defines providers to initialize the work manager library
 */
object WorkerInitializerModule {
  /**
   * Defines the [ScopeObserver] for work manager initialization in the [NamedScope] of [ForApp]
   */
  @Provide fun workerScopeInitializer(
    context: AppContext,
    configuration: Configuration? = null,
    defaultConfiguration: () -> @Default Configuration
  ): ScopeObserver<ForApp> = object : ScopeObserver<ForApp> {
    override fun onInit() {
      WorkManager.initialize(context, configuration ?: defaultConfiguration())
    }
  }

  /**
   * Defines the worker configuration which is used by [workerScopeInitializer] to initialize the [WorkManager]
   */
  @Provide fun defaultWorkerConfiguration(
    workerFactory: WorkerFactory
  ): @Default Configuration = Configuration.Builder()
    .setWorkerFactory(workerFactory)
    .build()

  @Tag private annotation class Default
}

@Provide inline val AppContext.workManager: WorkManager
  get() = WorkManager.getInstance(this)

internal typealias SingleWorkerFactory = (WorkerParameters) -> ListenableWorker
