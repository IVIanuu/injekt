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
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.ComponentOwner
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.keyOf

inline fun <reified T : Activity> ActivityComponent(
    instance: T,
    block: ComponentBuilder.() -> Unit = {}
): Component = ActivityComponent(instance, keyOf(), block)

inline fun <T : Activity> ActivityComponent(
    instance: T,
    key: Key<T>,
    block: ComponentBuilder.() -> Unit = {}
): Component = Component {
    scopes(ActivityScoped)
    instance.getClosestComponentOrNull()?.let { parents(it) }
    activityBindings(instance, key)
    block()
}

fun <T : Activity> ComponentBuilder.activityBindings(
    instance: T,
    key: Key<T>
) {
    com.ivianuu.injekt.instance(instance = instance, key = key)
    alias(key, keyOf<Activity>())
    if (instance is ComponentActivity) alias(key, keyOf<ComponentActivity>())
    if (instance is FragmentActivity) alias(key, keyOf<FragmentActivity>())
    if (instance is AppCompatActivity) alias(key, keyOf<AppCompatActivity>())

    (instance as? FragmentActivity)?.let {
        com.ivianuu.injekt.factory(duplicateStrategy = DuplicateStrategy.Override) { instance.supportFragmentManager }
        alias<FragmentManager>(aliasQualifier = ForActivity)
    }

    contextBindings(ForActivity) { instance }
    maybeLifecycleBindings(instance, ForActivity)
    maybeViewModelStoreBindings(instance, ForActivity)
    maybeSavedStateBindings(instance, ForActivity)
    componentAlias(ForActivity)
}

@Scope
annotation class ActivityScoped

@Qualifier
annotation class ForActivity

fun Activity.getClosestComponentOrNull(): Component? =
    getApplicationComponentOrNull()

fun Activity.getClosestComponent(): Component =
    getClosestComponentOrNull() ?: error("No close Component found for $this")

fun Activity.getApplicationComponentOrNull(): Component? =
    (application as? ComponentOwner)?.component

fun Activity.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: error("No application Component found for $this")
