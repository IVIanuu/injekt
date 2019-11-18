/*
 * Copyright 2019 Manuel Wrage
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
import com.ivianuu.injekt.component
import com.ivianuu.injekt.module
import com.ivianuu.injekt.typeOf

@Scope
annotation class ContentProviderScope

@Name(ForContentProvider.Companion::class)
annotation class ForContentProvider {
    companion object
}

fun <T : ContentProvider> T.contentProviderComponent(block: (ComponentBuilder.() -> Unit)? = null): Component =
    component {
        scopes<ContentProviderScope>()
        getClosestComponentOrNull()?.let { dependencies(it) }
        modules(contentProviderModule())
        block?.invoke(this)
    }

fun ContentProvider.getClosestComponentOrNull(): Component? =
    getApplicationComponentOrNull()

fun ContentProvider.getClosestComponent(): Component =
    getClosestComponentOrNull() ?: error("No close component found for $this")

fun ContentProvider.getApplicationComponentOrNull(): Component? =
    (context?.applicationContext as? InjektTrait)?.component

fun ContentProvider.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: error("No application component found for $this")

fun <T : ContentProvider> T.contentProviderModule(): Module = module {
    instance(
        this@contentProviderModule,
        typeOf(this@contentProviderModule)
    ).bindType<ContentProvider>()
}