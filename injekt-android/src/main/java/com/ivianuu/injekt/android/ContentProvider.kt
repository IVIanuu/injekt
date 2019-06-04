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

import android.content.ContentProvider
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Name
import com.ivianuu.injekt.NamedScope
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.ScopeAnnotation
import com.ivianuu.injekt.bindType
import com.ivianuu.injekt.constant.constant
import com.ivianuu.injekt.module

@ScopeAnnotation(ContentProviderScope.Companion::class)
annotation class ContentProviderScope {
    companion object : NamedScope("ContentProviderScope")
}

@Name(ForContentProvider.Companion::class)
annotation class ForContentProvider {
    companion object : Qualifier
}

fun <T : ContentProvider> T.contentProviderComponent(
    scope: Scope? = ContentProviderScope,
    modules: Iterable<Module> = emptyList(),
    dependencies: Iterable<Component> = emptyList()
): Component = androidComponent(
    scope, modules, dependencies,
    { contentProviderModule() },
    { getClosestComponentOrNull() }
)

fun ContentProvider.getClosestComponentOrNull(): Component? =
    getApplicationComponentOrNull()

fun ContentProvider.getClosestComponent(): Component =
    getClosestComponentOrNull() ?: error("No close component found for $this")

fun ContentProvider.getApplicationComponentOrNull(): Component? =
    (context?.applicationContext as? InjektTrait)?.component

fun ContentProvider.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: error("No application component found for $this")

fun <T : ContentProvider> T.contentProviderModule(): Module = module {
    constant(this@contentProviderModule) bindType ContentProvider::class
}