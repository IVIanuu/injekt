/*
 * Copyright 2021 Manuel Wrage
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

import android.content.*
import androidx.work.*
import com.ivianuu.injekt.*
import io.kotest.matchers.nulls.*
import io.mockk.*
import org.junit.*
import org.junit.runner.*
import org.robolectric.*
import org.robolectric.annotation.*

@Config(sdk = [28])
@RunWith(RobolectricTestRunner::class)
class InjektWorkerTest {
  @Test
  fun testWorkerBinding() {
    val workerFactory = inject<(@Provide Context) -> WorkerFactory>()(mockk())
    workerFactory.createWorker(mockk(), TestWorker::class.java.name, mockk())
      .shouldNotBeNull()
  }
}

@Provide @InjektWorker class TestWorker(
  appContext: Context,
  workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
  override fun doWork(): Result = Result.success()
}
