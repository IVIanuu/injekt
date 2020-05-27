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

package com.ivianuu.injekt.sample.proto

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ivianuu.injekt.InstanceFactory
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.create
import com.ivianuu.injekt.get
import com.ivianuu.injekt.internal.DelegateFactory
import com.ivianuu.injekt.sample.Database
import com.ivianuu.injekt.transient

@Module
fun <T : ListenableWorker> worker(provider: @Provider (Context, WorkerParameters) -> T) {
    transient { workerParameters: WorkerParameters ->
        provider(get(), workerParameters)
    }
}

@Module
fun <T> worker_(provider: @Provider (Context, WorkerParameters) -> T): workerClass<T> {
    return workerClass { context: Context, workerParameters: WorkerParameters ->
        provider(context, workerParameters)
    }
}

class workerClass<T>(
    val provider: @Provider (Context, WorkerParameters) -> T
)

@Module
fun myWorkerModule() {
    transient { Database(error("")) }
    worker { context, workerParameters ->
        MyWorker(context, workerParameters, get())
    }
}

@Module
fun myWorkerModule_(database: @Provider () -> Database): myWorkerModuleClass {
    val db = { Database(error("")) }
    val workerp1 = { context: Context, workerParameters: WorkerParameters, db: () -> Database ->
        MyWorker(context, workerParameters, db())
    }
    val worker = worker_ { c, p ->
        workerp1(c, p, database)
    }
    return myWorkerModuleClass(db, worker)
}

class myWorkerModuleClass(
    val database: @Provider () -> Database,
    val worker: workerClass<MyWorker>
)

class MyWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val database: Database
) : Worker(context, workerParams) {
    override fun doWork(): Result = Result.success()
}

@InstanceFactory
fun myWorkerFactory(): @Provider (Context, WorkerParameters) -> MyWorker {
    myWorkerModule()
    return create()
}

@InstanceFactory
fun myWorkerFactory_(): @Provider (Context, WorkerParameters) -> MyWorker {
    val dbProvider = DelegateFactory<Database>()
    val myWorkerModule = myWorkerModule_(dbProvider)
    dbProvider.setDelegate(myWorkerModule.database)

    return myWorkerModule.worker.provider
}
