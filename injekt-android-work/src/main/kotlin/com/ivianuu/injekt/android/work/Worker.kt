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
import com.ivianuu.injekt.ImplBinding
import com.ivianuu.injekt.MapEntries
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.merge.ApplicationComponent
import com.ivianuu.injekt.merge.BindingModule
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

@BindingModule(ApplicationComponent::class)
annotation class WorkerBinding {
    @Module
    class WorkerModule<T : (Context, WorkerParameters) -> ListenableWorker>(private val workerClass: KClass<out ListenableWorker>) {
        @MapEntries
        fun workerIntoMap(factory: T): Workers =
            mapOf(workerClass to factory)

        companion object {
            inline operator fun <reified T : (Context, WorkerParameters) -> ListenableWorker> invoke(): WorkerModule<T> {
                val workerClass =
                    typeOf<T>().arguments.last().type!!.classifier as KClass<out ListenableWorker>
                return WorkerModule(workerClass)
            }
        }
    }
}

typealias Workers = Map<KClass<out ListenableWorker>, (Context, WorkerParameters) -> ListenableWorker>

@MapEntries
fun defaultWorkers(): Workers = emptyMap()

@ImplBinding
class InjektWorkerFactory(private val workers: Workers) : WorkerFactory() {
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
