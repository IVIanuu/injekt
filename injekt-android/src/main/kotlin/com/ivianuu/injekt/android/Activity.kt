package com.ivianuu.injekt.android

import android.app.Activity
import androidx.activity.ComponentActivity
import com.ivianuu.injekt.ChildFactory
import com.ivianuu.injekt.CompositionFactory
import com.ivianuu.injekt.EntryPoint
import com.ivianuu.injekt.InstallIn
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.createImpl
import com.ivianuu.injekt.entryPointOf
import com.ivianuu.injekt.inject
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.parentFactory
import com.ivianuu.injekt.scope

@Scope
annotation class ActivityScoped

@Target(AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
@Qualifier
annotation class ForActivity

interface ActivityComponent

val ComponentActivity.activityComponent: ActivityComponent
    get() = lifecycle.singleton {
        entryPointOf<ActivityComponentFactoryOwner>(retainedActivityComponent)
            .activityComponentFactory(this)
    }

@CompositionFactory
fun createActivityComponent(instance: Activity): ActivityComponent {
    parentFactory(::createActivityComponent)
    scope<ActivityScoped>()
    instance(instance)
    return createImpl()
}

@InstallIn<:: createActivityComponent>
@EntryPoint
interface ActivityComponentFactoryOwner {
    val activityComponentFactory: @ChildFactory (Activity) -> ActivityComponent
}

fun <T : ComponentActivity> T.inject() {
    inject(activityComponent)
}
