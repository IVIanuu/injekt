package com.ivianuu.injekt.android

import androidx.activity.ComponentActivity
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.GivenSetElement
import com.ivianuu.injekt.component.AppComponent
import com.ivianuu.injekt.component.Component
import com.ivianuu.injekt.component.componentElement
import com.ivianuu.injekt.component.get

typealias ActivityRetainedComponent = Component

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
