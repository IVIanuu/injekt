package com.ivianuu.injekt.samples.android

import com.ivianuu.injekt.GivenSet
import com.ivianuu.injekt.android.ApplicationComponentKey
import com.ivianuu.injekt.android.ComponentElements
import com.ivianuu.injekt.android.componentElementsOf

object DummyAppElementKey : ApplicationComponentKey<() -> Unit>

@GivenSet fun dummyAppElement(): ComponentElements<ApplicationComponentKey<*>> =
    componentElementsOf(DummyAppElementKey) {
        println("Hello")
    }
