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
import com.ivianuu.injekt.android.*
import com.ivianuu.injekt.scope.*
import kotlin.reflect.*

/**
 * Installs the given [ListenableWorker] in the [InjektWorkerFactory]
 *
 * Example:
 * ```
 * @Given
 * @InstallWorker
 * class MyWorker(
 *     @Given context: AppContext,
 *     @Given parameters: WorkerParameters
 * ) : CoroutineWorker(context, parameters)
 * ```
 */
@Qualifier
annotation class InstallWorker {
    companion object {
        @Given
        inline fun <@Given T : @InstallWorker S, S : ListenableWorker> workerFactory(
            @Given noinline factory: (@Given WorkerParameters) -> T,
            @Given workerClass: KClass<S>
        ): Pair<String, SingleWorkerFactory> = workerClass.java.name to factory
    }
}

internal typealias SingleWorkerFactory = (WorkerParameters) -> ListenableWorker

/**
 * Factory which is able to create [ListenableWorker]s installed via [InstallWorker]
 */
@Given
class InjektWorkerFactory(
    @Given private val workers: Map<String, SingleWorkerFactory>
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = workers[workerClassName]?.invoke(workerParameters)
}

/**
 * Defines givens to initialize the work manager library
 */
@Given
object WorkerInitializer {
    /**
     * Defines the [GivenScopeInitializer] for work manager initialization in the [AppGivenScope]
     */
    @Given
    fun workerScopeInitializer(
        @Given context: AppContext,
        @Given configuration: Configuration? = null,
        @Given defaultConfiguration: () -> @Default Configuration
    ): GivenScopeInitializer<AppGivenScope> = {
        WorkManager.initialize(context, configuration ?: defaultConfiguration())
    }

    /**
     * Defines the worker configuration which is used by [workerScopeInitializer] to initialize the [WorkManager]
     */
    @Given
    fun workerConfiguration(
        @Given workerFactory: WorkerFactory
    ): @Default Configuration = Configuration.Builder()
        .setWorkerFactory(workerFactory)
        .build()

    @Qualifier
    private annotation class Default
}

@Given
inline val AppContext.workManager: WorkManager
    get() = WorkManager.getInstance(this)
