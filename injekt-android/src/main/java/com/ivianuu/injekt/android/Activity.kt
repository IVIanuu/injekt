/*
 * Copyright 2018 Manuel Wrage
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
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Name
import com.ivianuu.injekt.NamedScope
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.ScopeAnnotation
import com.ivianuu.injekt.component
import com.ivianuu.injekt.constant.constant
import com.ivianuu.injekt.module

@ScopeAnnotation(ActivityScope.Companion::class)
annotation class ActivityScope {
    companion object : NamedScope("ActivityScope")
}

@Name(ForActivity.Companion::class)
annotation class ForActivity {
    companion object : Qualifier
}

fun <T : Activity> T.activityComponent(
    block: (ComponentBuilder.() -> Unit)? = null
): Component = component {
    scope = ActivityScope
    getClosestComponentOrNull()?.let { dependencies(it) }
    modules(activityModule())
    block?.invoke(this)
}

fun Activity.getClosestComponentOrNull(): Component? =
    getApplicationComponentOrNull()

fun Activity.getClosestComponent(): Component =
    getClosestComponentOrNull() ?: error("No close component found for $this")

fun Activity.getApplicationComponentOrNull(): Component? = (application as? InjektTrait)?.component

fun Activity.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: error("No application component found for $this")

fun <T : Activity> T.activityModule(): Module = module {
    constant(this@activityModule, override = true)/*.apply {
        bindType<Activity>()
        bindAlias<Context>(ForActivity)
        bindType<Context>()

        (this@activityModule as? ComponentActivity)?.let { bindType<ComponentActivity>() }
        (this@activityModule as? FragmentActivity)?.let { bindType<ComponentActivity>() }
        (this@activityModule as? AppCompatActivity)?.let { bindType<AppCompatActivity>() }
    }

    factory(override = true) { resources } bindName ForActivity

    (this@activityModule as? LifecycleOwner)?.let {
        constant(this@activityModule, type = typeOf<LifecycleOwner>()) bindName ForActivity
        factory { lifecycle } bindName ForActivity
    }

    (this@activityModule as? ViewModelStoreOwner)?.let {
        constant(this@activityModule, type = typeOf<ViewModelStoreOwner>()) bindName ForActivity
        factory { viewModelStore } bindName ForActivity
    }

    (this@activityModule as? SavedStateRegistryOwner)?.let {
        constant(this@activityModule, type = typeOf<SavedStateRegistryOwner>()) bindName ForActivity
        factory { savedStateRegistry } bindName ForActivity
    }

    (this@activityModule as? FragmentActivity)?.let {
        factory { supportFragmentManager } bindName ForActivity
    }*/
}