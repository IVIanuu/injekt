package com.ivianuu.injekt.samples.android

import com.ivianuu.injekt.GivenSet
import com.ivianuu.injekt.component.ApplicationScoped
import com.ivianuu.injekt.component.Component
import com.ivianuu.injekt.component.componentElementsOf

object DummyAppElementKey : Component.Key<() -> Unit>

@GivenSet fun dummyAppElement() =
    componentElementsOf(ApplicationScoped, DummyAppElementKey) {
        println("Hello")
    }
