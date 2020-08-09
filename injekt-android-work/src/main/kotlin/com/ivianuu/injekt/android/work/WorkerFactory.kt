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
import com.ivianuu.injekt.Effect
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.MapEntries
import com.ivianuu.injekt.given
import kotlin.reflect.KClass

@Effect
annotation class BindWorker {
    companion object {
        @MapEntries
        inline operator fun <reified T : ListenableWorker> invoke(): Workers = mapOf(
            T::class to given<(Context, WorkerParameters) -> T>()
        )
    }
}

typealias Workers = Map<KClass<out ListenableWorker>, (Context, WorkerParameters) -> ListenableWorker>

@Given
internal class InjektWorkerFactory : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return given<Workers>()[Class.forName(workerClassName).kotlin]?.invoke(
            appContext,
            workerParameters
        ) ?: error("Could not find a worker for $workerClassName")
    }
}

object WorkerInjectionModule {
    @Given
    fun workerFactory(): WorkerFactory = given<InjektWorkerFactory>()
}
