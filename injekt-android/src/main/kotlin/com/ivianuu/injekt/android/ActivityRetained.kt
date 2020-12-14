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
import com.ivianuu.injekt.given

@Given object ActivityRetainedScoped : Component.Name

@Given val @Given ComponentActivity.activityRetainedComponent: Component<ActivityRetainedScoped>
    get() = viewModelStore.component {
        application.applicationComponent[ActivityRetainedComponentFactoryKey]()
    }

private val ActivityRetainedComponentFactoryKey =
    ComponentKey<() -> Component<ActivityRetainedScoped>>()

@GivenSetElement fun activityRetainedComponentFactory(
    builderFactory: () -> Component.Builder<ActivityRetainedScoped> = given,
) = componentElement(ApplicationScoped, ActivityRetainedComponentFactoryKey) {
    builderFactory().build()
}

@Given val @Given Component<ActivityRetainedScoped>.applicationComponentFromRetained: Component<ApplicationScoped>
    get() = getDependency(ApplicationScoped)
