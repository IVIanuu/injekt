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
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.QualifierMarker
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.keyOf

inline fun <reified T : ContentProvider> ContentProviderComponent(
    instance: T,
    block: ComponentBuilder.() -> Unit = {}
): Component = ContentProviderComponent(instance = instance, key = keyOf(), block = block)

inline fun <T : ContentProvider> ContentProviderComponent(
    instance: T,
    key: Key<T>,
    block: ComponentBuilder.() -> Unit = {}
): Component =
    Component {
        scopes(ContentProviderScope)
        instance.getClosestComponentOrNull()?.let { dependencies(it) }

        instance(instance, key = key)
            .bindAlias<ContentProvider>()
        contextBindings(ForContentProvider) { instance.context!! }
        componentAlias(ForContentProvider)

        block()
    }

@Scope
annotation class ContentProviderScope {
    companion object
}

@QualifierMarker
annotation class ForContentProvider {
    companion object : Qualifier.Element
}

fun ContentProvider.getClosestComponentOrNull(): Component? =
    getApplicationComponentOrNull()

fun ContentProvider.getClosestComponent(): Component =
    getClosestComponentOrNull() ?: error("No close Component found for $this")

fun ContentProvider.getApplicationComponentOrNull(): Component? =
    (context?.applicationContext as? InjektTrait)?.component

fun ContentProvider.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: error("No application Component found for $this")
