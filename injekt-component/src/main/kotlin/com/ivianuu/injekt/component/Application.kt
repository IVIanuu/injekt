package com.ivianuu.injekt.component

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.given

object ApplicationScoped : Component.Name

fun App.initializeApp(elements: (Component<ApplicationScoped>) -> Set<ComponentElement<ApplicationScoped>> = given) {
    _applicationComponent = ComponentBuilder(ApplicationScoped, elements)
        .element(ApplicationKey, this)
        .build()
}

typealias App = Any

@Given val @Given Component<ApplicationScoped>.app: App
    get() = this[ApplicationKey]

private val ApplicationKey = ComponentKey<App>()

private lateinit var _applicationComponent: Component<ApplicationScoped>
@Given val @Given App.applicationComponent: Component<ApplicationScoped> get() = _applicationComponent
