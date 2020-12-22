package com.ivianuu.injekt.samples.android

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.common.ForKey
import com.ivianuu.injekt.component.Component
import com.ivianuu.injekt.component.scope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

typealias ComponentCoroutineScope<C> = CoroutineScope

@Given fun <@ForKey C : Component> storageCoroutineScope(@Given component: C): ComponentCoroutineScope<C> =
    component.scope {
        object : CoroutineScope, DisposableHandle {
            override val coroutineContext: CoroutineContext = Job() + Dispatchers.Default
            override fun dispose() {
                coroutineContext.cancel()
            }
        }
    }
