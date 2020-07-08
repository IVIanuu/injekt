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
import com.ivianuu.injekt.ApplicationComponent
import com.ivianuu.injekt.MapEntries
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.Unscoped
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.composition.BindingAdapter
import com.ivianuu.injekt.composition.installIn
import com.ivianuu.injekt.map
import com.ivianuu.injekt.unscoped
import kotlin.reflect.KClass

@BindingAdapter(ApplicationComponent::class)
annotation class BindWorker {
    companion object {
        @Module
        inline operator fun <reified T : ListenableWorker> invoke() {
            unscoped<T>()
            map<KClass<out ListenableWorker>, @Provider (Context, WorkerParameters) -> ListenableWorker> {
                put<@Provider (Context, WorkerParameters) -> T>(T::class)
            }
        }
    }
}

@Unscoped
internal class InjektWorkerFactory(
    private val workers: Map<KClass<out ListenableWorker>, @Provider (Context, WorkerParameters) -> ListenableWorker>
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return workers[Class.forName(workerClassName).kotlin]?.invoke(appContext, workerParameters)
            ?: error("Could not find a worker for $workerClassName")
    }
}

@Module
fun WorkerInjectionModule() {
    installIn<ApplicationComponent>()
    map<KClass<out ListenableWorker>, ListenableWorker>()
    alias<InjektWorkerFactory, WorkerFactory>()
}
