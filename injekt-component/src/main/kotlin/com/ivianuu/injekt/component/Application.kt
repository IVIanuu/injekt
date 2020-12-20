package com.ivianuu.injekt.component

import com.ivianuu.injekt.Given

object ApplicationScoped : Component.Name

fun App.initializeApp(
    @Given elementsFactory: (Component<ApplicationScoped>) -> Set<ComponentElement<ApplicationScoped>>
) {
    _applicationComponent = ComponentBuilder(ApplicationScoped, elementsFactory)
        .element(ApplicationKey, this)
        .build()
}

typealias App = Any

@Given val @Given Component<ApplicationScoped>.app: App
    get() = this[ApplicationKey]

private val ApplicationKey = ComponentKey<App>()

private lateinit var _applicationComponent: Component<ApplicationScoped>
@Given val @Given App.applicationComponent: Component<ApplicationScoped>
    get() = _applicationComponent
