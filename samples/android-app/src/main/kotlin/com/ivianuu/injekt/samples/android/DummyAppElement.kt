package com.ivianuu.injekt.samples.android

import com.ivianuu.injekt.GivenSetElement
import com.ivianuu.injekt.component.ApplicationScoped
import com.ivianuu.injekt.component.Component
import com.ivianuu.injekt.component.componentElement

object DummyAppElementKey : Component.Key<() -> Unit>

@GivenSetElement fun dummyAppElement() = componentElement(ApplicationScoped, DummyAppElementKey) {
    println("Hello")
}
