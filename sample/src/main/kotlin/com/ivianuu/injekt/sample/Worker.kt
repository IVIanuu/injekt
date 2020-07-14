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

package com.ivianuu.injekt.sample

import android.content.Context
import androidx.work.Configuration
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.android.ApplicationContext
import com.ivianuu.injekt.android.work.BindWorker
import com.ivianuu.injekt.given

@BindWorker
@Given
@Reader
class TestWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    init {
        println("hello $context $workerParams ${given<Repo>()}")
    }

    override suspend fun doWork(): Result = Result.success()

}

@Reader
fun initializeWorkers() {
    WorkManager.initialize(
        given<ApplicationContext>(), Configuration.Builder()
            .setWorkerFactory(given()).build()
    )
}
