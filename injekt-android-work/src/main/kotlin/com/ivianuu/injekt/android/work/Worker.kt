/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.android.work

import android.content.*
import androidx.work.*
import com.ivianuu.injekt.*
import kotlin.reflect.*

class WorkerModule<T : ListenableWorker> {
  @Provide inline fun workerFactory(
    noinline factory: (Context, WorkerParameters) -> T,
    workerClass: KClass<T>
  ): Pair<String, SingleWorkerFactory> = workerClass.java.name to SingleWorkerFactory(factory)
}

/**
 * Factory which is able to create [ListenableWorker]s installed via [WorkerModule]
 */
@Provide class InjektWorkerFactory(private val workers: Map<String, SingleWorkerFactory>) : WorkerFactory() {
  override fun createWorker(
    appContext: Context,
    workerClassName: String,
    workerParameters: WorkerParameters,
  ): ListenableWorker? = workers[workerClassName]?.value?.invoke(appContext, workerParameters)
}

@JvmInline value class SingleWorkerFactory(val value: (Context, WorkerParameters) -> ListenableWorker) {
  companion object {
    @Provide val defaultWorkers: Collection<Pair<String, SingleWorkerFactory>> get() = emptyList()
  }
}
