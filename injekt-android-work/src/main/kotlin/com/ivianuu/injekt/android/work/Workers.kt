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
import com.ivianuu.injekt.ContextBuilder
import com.ivianuu.injekt.ForKey
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.common.Adapter
import com.ivianuu.injekt.common.ApplicationContext
import com.ivianuu.injekt.common.toKeyInfo
import com.ivianuu.injekt.given
import com.ivianuu.injekt.keyOf
import com.ivianuu.injekt.scopedGiven
import kotlin.reflect.KClass

@Adapter(ApplicationContext::class)
annotation class GivenWorker {
    companion object : Adapter.Impl<@Reader (Context, WorkerParameters) -> ListenableWorker> {
        override fun ContextBuilder.configure(
            key: Key<@Reader (Context, WorkerParameters) -> ListenableWorker>,
            provider: () -> @Reader (Context, WorkerParameters) -> ListenableWorker
        ) {
            @Suppress("UNCHECKED_CAST")
            givenWorker(key.toKeyInfo().arguments[2].key as Key<ListenableWorker>, provider())
        }
    }
}

fun <@ForKey T : ListenableWorker> ContextBuilder.givenWorker(
    key: Key<T> = keyOf(),
    provider: @Reader (Context, WorkerParameters) -> T
) {
    map(keyOf<Workers>()) {
        put(key.toKeyInfo().classifier) { provider }
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
        return given<Workers>()[Class.forName(workerClassName).kotlin]?.invoke(
            appContext,
            workerParameters
        )
    }
}

typealias Workers = Map<KClass<*>, @Reader (Context, WorkerParameters) -> ListenableWorker>
