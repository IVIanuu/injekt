package com.ivianuu.injekt.component

import com.ivianuu.injekt.Given

typealias AppComponent = Component

fun App.initializeApp(
    @Given elementsFactory: (@Given AppComponent) -> Set<ComponentElement<AppComponent>>
) {
    _appComponent = ComponentBuilder(elementsFactory)
        .element(this)
        .build()
}

typealias App = Any

@Given val @Given AppComponent.app: App
    get() = get()

private lateinit var _appComponent: AppComponent
@Given val @Given App.appComponent: AppComponent
    get() = _appComponent
