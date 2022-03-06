/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("NOTHING_TO_INLINE")

package com.ivianuu.injekt.android.work

import android.content.*
import androidx.work.*
import com.ivianuu.injekt.*
import kotlin.reflect.*

class WorkerModule<T : ListenableWorker> {
  @Provide fun workerFactory(
    factory: (Context, WorkerParameters) -> T,
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
  @Provide companion object {
    @Provide inline val defaultWorkers: Collection<Pair<String, SingleWorkerFactory>> get() = emptyList()
  }
}

@Provide object WorkManagerModule {
  @Provide inline fun workManager(context: Context): WorkManager = WorkManager.getInstance(context)
}
