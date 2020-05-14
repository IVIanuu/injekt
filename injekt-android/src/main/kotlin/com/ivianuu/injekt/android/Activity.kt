/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.parent
import com.ivianuu.injekt.scope

@Scope
annotation class ActivityScoped

@Target(AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
@Qualifier
annotation class ForActivity

interface ActivityComponent

val ComponentActivity.activityComponent: ActivityComponent
    get() = lifecycle.singleton {
        entryPointOf<ActivityComponentFactoryOwner>(
            retainedActivityComponent
        ).activityComponentFactory(this)
    }

@CompositionFactory
fun createActivityComponent(instance: Activity): ActivityComponent {
    parent<ApplicationComponent>()
    scope<ActivityScoped>()
    instance(instance)
    return createImpl()
}

@InstallIn<ApplicationComponent>
@EntryPoint
interface ActivityComponentFactoryOwner {
    val activityComponentFactory: @ChildFactory (Activity) -> ActivityComponent
}
