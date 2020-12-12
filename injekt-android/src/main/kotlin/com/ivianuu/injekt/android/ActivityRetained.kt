package com.ivianuu.injekt.android

import androidx.activity.ComponentActivity
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.GivenSet
import com.ivianuu.injekt.given

typealias ActivityRetainedComponent = Component<ActivityRetainedComponentKey<*>>

interface ActivityRetainedComponentKey<T> : Component.Key<T>

@GivenSet fun defaultActivityRetainedComponentElements(): ComponentElements<ActivityRetainedComponentKey<*>> =
    emptyMap()

@Given fun activityRetainedComponent(
    activity: ComponentActivity = given,
    applicationComponent: ApplicationComponent = given,
) = activity.viewModelStore.component {
    applicationComponent[ActivityRetainedComponentFactoryKey]()
}

object ActivityRetainedComponentFactoryKey :
    ApplicationComponentKey<() -> ActivityRetainedComponent>

@GivenSet fun activityRetainedComponentFactory(
    builderFactory: () -> Component.Builder<ActivityRetainedComponentKey<*>> = given,
): ComponentElements<ApplicationComponentKey<*>> =
    componentElementsOf(ActivityRetainedComponentFactoryKey) {
        builderFactory()
            .build()
    }

typealias ActivityRetainedStorage = Storage

@Given fun activityRetainedStorage(component: ActivityRetainedComponent = given): ActivityRetainedStorage =
    component.storage
