package com.ivianuu.injekt.sample

import android.content.Context
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope

@Scope
annotation class ApplicationScoped

@Scope
annotation class RetainedActivityScoped

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
@Qualifier
annotation class ForApplication

abstract class Worker(
    context: Context,
    workerParameters: WorkerParameters
)

interface WorkerParameters

abstract class WorkerFactory {
    abstract fun create(
        className: String,
        context: Context,
        workerParameters: WorkerParameters
    ): Worker
}