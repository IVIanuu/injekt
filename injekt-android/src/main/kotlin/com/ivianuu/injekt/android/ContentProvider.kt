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

import android.content.ContentProvider
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Name
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.Type
import com.ivianuu.injekt.typeOf

inline fun <reified T : ContentProvider> ContentProviderComponent(
    instance: T,
    block: ComponentBuilder.() -> Unit = {}
): Component = ContentProviderComponent(instance = instance, type = typeOf(), block = block)

inline fun <T : ContentProvider> ContentProviderComponent(
    instance: T,
    type: Type<T>,
    block: ComponentBuilder.() -> Unit = {}
): Component =
    Component {
        scopes(ContentProviderScope)
        instance.getClosestComponentOrNull()?.let { dependencies(it) }
        modules(ContentProviderModule(instance, type))
        block()
    }

inline fun <reified T : ContentProvider> ContentProviderModule(
    instance: T
): Module = ContentProviderModule(instance = instance, type = typeOf())

fun <T : ContentProvider> ContentProviderModule(
    instance: T,
    type: Type<T>
): Module = Module {
    instance(instance, type = type)
        .bindAlias<ContentProvider>()

    factory(override = true) { instance.context!! }
        .bindAlias(name = ForContentProvider)

    withBinding<Component>(name = ContentProviderScope) {
        bindAlias(name = ForContentProvider)
    }
}

@Scope
annotation class ContentProviderScope {
    companion object
}

@Name
annotation class ForContentProvider {
    companion object
}

fun ContentProvider.getClosestComponentOrNull(): Component? =
    getApplicationComponentOrNull()

fun ContentProvider.getClosestComponent(): Component =
    getClosestComponentOrNull() ?: error("No close Component found for $this")

fun ContentProvider.getApplicationComponentOrNull(): Component? =
    (context?.applicationContext as? InjektTrait)?.component

fun ContentProvider.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: error("No application Component found for $this")
