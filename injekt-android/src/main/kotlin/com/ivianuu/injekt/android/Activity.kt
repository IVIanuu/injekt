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
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Name
import com.ivianuu.injekt.OverrideStrategy
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.Type
import com.ivianuu.injekt.typeOf

inline fun <reified T : Activity> ActivityComponent(
    instance: T,
    block: ComponentBuilder.() -> Unit = {}
): Component = ActivityComponent(instance = instance, type = typeOf(), block = block)

inline fun <T : Activity> ActivityComponent(
    instance: T,
    type: Type<T>,
    block: ComponentBuilder.() -> Unit = {}
): Component = Component {
    scopes(ActivityScope)
    instance.getClosestComponentOrNull()?.let { dependencies(it) }
    modules(ActivityModule(instance, type))
    block()
}

inline fun <reified T : Activity> ActivityModule(
    instance: T
): Module = ActivityModule(instance = instance, type = typeOf())

fun <T : Activity> ActivityModule(
    instance: T,
    type: Type<T>
): Module = Module {
    instance(instance = instance, type = type).apply {
        bindAlias<Activity>()
        bindAlias<Context>(name = ForActivity, overrideStrategy = OverrideStrategy.Override)
        bindAlias<Context>(overrideStrategy = OverrideStrategy.Override)

        if (instance is ComponentActivity) bindAlias<ComponentActivity>()
        if (instance is FragmentActivity) bindAlias<FragmentActivity>()
        if (instance is AppCompatActivity) bindAlias<AppCompatActivity>()
        if (instance is LifecycleOwner) {
            bindAlias<LifecycleOwner>()
            bindAlias<LifecycleOwner>(name = ForActivity)
        }
        if (instance is ViewModelStoreOwner) {
            bindAlias<ViewModelStoreOwner>()
            bindAlias<ViewModelStoreOwner>(name = ForActivity)
        }
        if (instance is SavedStateRegistryOwner) {
            bindAlias<SavedStateRegistryOwner>()
            bindAlias<SavedStateRegistryOwner>(name = ForActivity)
        }
    }

    factory(overrideStrategy = OverrideStrategy.Override) { instance.resources!! }
        .bindAlias(name = ForActivity)

    (instance as? LifecycleOwner)?.let {
        factory(overrideStrategy = OverrideStrategy.Override) { instance.lifecycle }
            .bindAlias(name = ForActivity)
    }

    (instance as? ViewModelStoreOwner)?.let {
        factory(overrideStrategy = OverrideStrategy.Override) { instance.viewModelStore }
            .bindAlias(name = ForActivity)
    }

    (instance as? SavedStateRegistryOwner)?.let {
        factory(overrideStrategy = OverrideStrategy.Override) { instance.savedStateRegistry }
            .bindAlias(name = ForActivity)
    }

    (instance as? FragmentActivity)?.let {
        factory(overrideStrategy = OverrideStrategy.Override) { instance.supportFragmentManager }
            .bindAlias(name = ForActivity)
    }

    withBinding<Component>(name = ActivityScope) {
        bindAlias(name = ForActivity)
    }
}

@Scope
annotation class ActivityScope {
    companion object
}

@Name
annotation class ForActivity {
    companion object
}

fun Activity.getClosestComponentOrNull(): Component? =
    getApplicationComponentOrNull()

fun Activity.getClosestComponent(): Component =
    getClosestComponentOrNull() ?: error("No close Component found for $this")

fun Activity.getApplicationComponentOrNull(): Component? = (application as? InjektTrait)?.component

fun Activity.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: error("No application Component found for $this")