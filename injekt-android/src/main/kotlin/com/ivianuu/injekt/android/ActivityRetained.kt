package com.ivianuu.injekt.android

import androidx.activity.ComponentActivity
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.GivenSet
import com.ivianuu.injekt.component.Component
import com.ivianuu.injekt.component.componentElementsOf
import com.ivianuu.injekt.given

object ActivityRetainedScoped : Component.Name

@Given fun activityRetainedComponent(
    activity: ComponentActivity = given,
    applicationComponent: Component<ApplicationScoped> = given,
) = activity.viewModelStore.component {
    applicationComponent[ActivityRetainedComponentFactoryKey]()
}

object ActivityRetainedComponentFactoryKey : Component.Key<() -> Component<ActivityRetainedScoped>>

@GivenSet fun activityRetainedComponentFactory(
    builderFactory: () -> Component.Builder<ActivityRetainedScoped> = given,
) = componentElementsOf(ActivityRetainedScoped::class, ActivityRetainedComponentFactoryKey) {
    builderFactory().build()
}
