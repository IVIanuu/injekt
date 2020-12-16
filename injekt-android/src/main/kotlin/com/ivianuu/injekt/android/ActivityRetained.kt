package com.ivianuu.injekt.android

import androidx.activity.ComponentActivity
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.GivenSetElement
import com.ivianuu.injekt.component.ApplicationScoped
import com.ivianuu.injekt.component.Component
import com.ivianuu.injekt.component.ComponentKey
import com.ivianuu.injekt.component.componentElement
import com.ivianuu.injekt.component.get
import com.ivianuu.injekt.component.getDependency

@Given object ActivityRetainedScoped : Component.Name

@Given val @Given ComponentActivity.activityRetainedComponent: Component<ActivityRetainedScoped>
    get() = viewModelStore.component {
        application.applicationComponent[ActivityRetainedComponentFactoryKey]()
    }

private val ActivityRetainedComponentFactoryKey =
    ComponentKey<() -> Component<ActivityRetainedScoped>>()

@GivenSetElement fun activityRetainedComponentFactory(
    @Given parent: Component<ApplicationScoped>,
    @Given builderFactory: () -> Component.Builder<ActivityRetainedScoped>,
) = componentElement(ApplicationScoped, ActivityRetainedComponentFactoryKey) {
    builderFactory().dependency(parent).build()
}

@Given val @Given Component<ActivityRetainedScoped>.applicationComponentFromRetained: Component<ApplicationScoped>
    get() = getDependency(ApplicationScoped)
