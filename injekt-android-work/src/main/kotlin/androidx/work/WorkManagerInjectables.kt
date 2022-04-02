/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package androidx.work

import android.content.Context
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.android.work.InjektWorkerFactory

object WorkManagerInjectables {
  @Provide inline fun workManager(context: Context): WorkManager = WorkManager.getInstance(context)

  @Provide inline fun workerFactory(factory: InjektWorkerFactory): WorkerFactory = factory
}
