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
import com.ivianuu.injekt.ApplicationContext
import com.ivianuu.injekt.ContextBuilder
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.given
import com.ivianuu.injekt.scopedGiven
import kotlin.reflect.KClass

inline fun <reified T : ListenableWorker> ContextBuilder.givenWorker(
    noinline provider: @Reader (Context, WorkerParameters) -> T
) {
    map(Workers) {
        put(T::class) { provider }
    }
}

@Module(ApplicationContext::class)
fun ContextBuilder.workerInjection() {
    scopedGiven<WorkerFactory> { InjektWorkerFactory() }
}

@Reader
internal class InjektWorkerFactory : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return given(Workers)[Class.forName(workerClassName).kotlin]?.invoke(
            appContext,
            workerParameters
        )
    }
}

object Workers : Key<Map<KClass<*>, @Reader (Context, WorkerParameters) -> ListenableWorker>>
