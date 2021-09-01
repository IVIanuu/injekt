package androidx.work

import android.content.Context
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.android.work.InjektWorkerFactory

@Provide inline val Context.workManager: WorkManager
  get() = WorkManager.getInstance(this)

@Provide inline val InjektWorkerFactory.workerFactory: WorkerFactory
  get() = this
