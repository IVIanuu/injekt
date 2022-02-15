/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.android.work

import android.content.*
import androidx.work.*
import com.ivianuu.injekt.*
import io.kotest.matchers.types.*
import io.mockk.*
import org.junit.*
import org.junit.runner.*
import org.robolectric.*
import org.robolectric.annotation.*

@Config(sdk = [28])
@RunWith(RobolectricTestRunner::class)
class WorkerModuleTest {
  @Test fun testWorkerModule() {
    val workerFactory = inject<(Context) -> WorkerFactory>()(mockk())
    workerFactory.createWorker(mockk(), TestWorker::class.java.name, mockk())
      .shouldBeTypeOf<TestWorker>()
  }
}

@Provide class TestWorker(
  appContext: Context,
  workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
  override fun doWork(): Result = Result.success()

  companion object {
    @Provide val module = WorkerModule<TestWorker>()
  }
}
