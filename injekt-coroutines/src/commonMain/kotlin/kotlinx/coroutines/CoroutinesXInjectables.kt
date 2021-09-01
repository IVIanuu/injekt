package kotlinx.coroutines

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.scope.Disposer

@Provide val coroutineScopeDisposer = Disposer<CoroutineScope> { it.cancel() }
@Provide val jobDisposer = Disposer<Job> { it.cancel() }
