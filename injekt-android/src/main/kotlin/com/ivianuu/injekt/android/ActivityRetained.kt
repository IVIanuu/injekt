package com.ivianuu.injekt.android

import androidx.activity.ComponentActivity
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.GivenSet
import com.ivianuu.injekt.component.Component
import com.ivianuu.injekt.component.componentElementsOf
import com.ivianuu.injekt.given

object ActivityRetainedScoped : Component.Name

@Given val @Given ComponentActivity.activityRetainedComponent: Component<ActivityRetainedScoped>
    get() = viewModelStore.component {
        application.applicationComponent[ActivityRetainedComponentFactoryKey]()
    }

private object ActivityRetainedComponentFactoryKey :
    Component.Key<() -> Component<ActivityRetainedScoped>>

@GivenSet fun activityRetainedComponentFactory(
    builderFactory: () -> Component.Builder<ActivityRetainedScoped> = given,
) = componentElementsOf(ApplicationScoped::class, ActivityRetainedComponentFactoryKey) {
    builderFactory().build()
}
