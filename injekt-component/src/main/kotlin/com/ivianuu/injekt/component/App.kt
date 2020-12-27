package com.ivianuu.injekt.component

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Unqualified
import com.ivianuu.injekt.common.ForKey

typealias AppComponent = Component

@Qualifier annotation class AppScoped
@Given inline fun <@ForKey T : Any> appScoped(
    @Given component: AppComponent,
    @Given factory: () -> @AppScoped T
): @Unqualified T = component.scope(factory)

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
