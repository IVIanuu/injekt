package com.ivianuu.injekt.android

import androidx.activity.ComponentActivity
import com.ivianuu.injekt.merge.ApplicationComponent
import com.ivianuu.injekt.merge.MergeChildComponent
import com.ivianuu.injekt.merge.MergeInto
import com.ivianuu.injekt.merge.mergeComponent

@MergeChildComponent
abstract class RetainedActivityComponent

val ComponentActivity.retainedActivityComponent: RetainedActivityComponent
    get() = viewModelStore.singleton {
        application.applicationComponent
            .mergeComponent<RetainedActivityComponentFactoryOwner>()
            .retainedActivityComponentFactory()
    }

@MergeInto(ApplicationComponent::class)
interface RetainedActivityComponentFactoryOwner {
    val retainedActivityComponentFactory: () -> RetainedActivityComponent
}
