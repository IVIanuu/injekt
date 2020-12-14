package com.ivianuu.injekt.android

import androidx.activity.ComponentActivity
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.GivenSetElement
import com.ivianuu.injekt.component.ApplicationScoped
import com.ivianuu.injekt.component.Component
import com.ivianuu.injekt.component.componentElement
import com.ivianuu.injekt.given

object ActivityRetainedScoped : Component.Name

@Given val @Given ComponentActivity.activityRetainedComponent: Component<ActivityRetainedScoped>
    get() = viewModelStore.component {
        application.applicationComponent[ActivityRetainedComponentFactoryKey]()
    }

private object ActivityRetainedComponentFactoryKey :
    Component.Key<() -> Component<ActivityRetainedScoped>>

@GivenSetElement fun activityRetainedComponentFactory(
    builderFactory: () -> Component.Builder<ActivityRetainedScoped> = given,
) = componentElement(ApplicationScoped, ActivityRetainedComponentFactoryKey) {
    builderFactory().build()
}
