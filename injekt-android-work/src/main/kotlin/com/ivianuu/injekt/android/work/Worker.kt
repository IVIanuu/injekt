/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.android.work

import android.content.*
import androidx.work.*
import com.ivianuu.injekt.*
import kotlin.reflect.*

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

    @Provide val defaultWorkers: Collection<Pair<String, SingleWorkerFactory>> get() = emptyList()
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
  ): ListenableWorker? = workers[workerClassName]?.invoke(appContext, workerParameters)
}

@Tag private annotation class SingleWorkerFactoryTag
private typealias SingleWorkerFactory = @SingleWorkerFactoryTag (Context, WorkerParameters) -> ListenableWorker
