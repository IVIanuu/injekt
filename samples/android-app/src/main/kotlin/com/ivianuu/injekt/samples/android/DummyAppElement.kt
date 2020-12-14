package com.ivianuu.injekt.samples.android

import com.ivianuu.injekt.GivenSetElement
import com.ivianuu.injekt.component.ApplicationScoped
import com.ivianuu.injekt.component.ComponentKey
import com.ivianuu.injekt.component.componentElement

val DummyAppElementKey = ComponentKey<() -> Unit>()
@GivenSetElement fun dummyAppElement() = componentElement(ApplicationScoped, DummyAppElementKey) {
    println("Hello")
}
