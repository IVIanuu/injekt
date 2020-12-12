package com.ivianuu.injekt.samples.android

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.android.Storage
import com.ivianuu.injekt.android.memo
import com.ivianuu.injekt.given
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

typealias StorageCoroutineScope<S> = CoroutineScope

@Given fun <S : Storage> storageCoroutineScope(storage: S = given): StorageCoroutineScope<S> =
    storage.memo("coroutine_scope") {
        object : CoroutineScope, DisposableHandle {
            override val coroutineContext: CoroutineContext = Job() + Dispatchers.Default
            override fun dispose() {
                coroutineContext.cancel()
            }
        }
    }
