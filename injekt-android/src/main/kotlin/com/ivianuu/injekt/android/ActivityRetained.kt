package com.ivianuu.injekt.android

import androidx.activity.ComponentActivity
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.GivenSetElement
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Unqualified
import com.ivianuu.injekt.common.ForKey
import com.ivianuu.injekt.component.AppComponent
import com.ivianuu.injekt.component.Component
import com.ivianuu.injekt.component.componentElement
import com.ivianuu.injekt.component.get
import com.ivianuu.injekt.component.scope

typealias ActivityRetainedComponent = Component

@Qualifier annotation class ActivityRetainedScoped
@Given inline fun <@ForKey T : Any> activityRetainedScoped(
    @Given component: ActivityRetainedComponent,
    @Given factory: () -> @ActivityRetainedScoped T
): @Unqualified T = component.scope(factory)

@Given val @Given ComponentActivity.activityRetainedComponent: ActivityRetainedComponent
    get() = viewModelStore.component {
        application.appComponent.get<() -> ActivityRetainedComponent>()()
    }

@GivenSetElement fun activityRetainedComponentFactory(
    @Given parent: AppComponent,
    @Given builderFactory: () -> Component.Builder<ActivityRetainedComponent>,
) = componentElement<AppComponent, () -> ActivityRetainedComponent> {
    builderFactory().dependency(parent).build()
}

@Given val @Given ActivityRetainedComponent.appComponentFromRetained: AppComponent
    get() = get()
