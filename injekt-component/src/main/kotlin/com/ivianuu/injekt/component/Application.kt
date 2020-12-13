package com.ivianuu.injekt.component

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.given

object ApplicationScoped : Component.Name

fun App.initializeApp(builder: Component.Builder<ApplicationScoped> = given) {
    _applicationComponent = builder
        .set(ApplicationKey, this)
        .build()
}

typealias App = Any

@Given val @Given Component<ApplicationScoped>.app: App
    get() = this[ApplicationKey]

private object ApplicationKey : Component.Key<App>

@Given lateinit var _applicationComponent: Component<ApplicationScoped>
