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

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.ivianuu.injekt.ApplicationScope
import com.ivianuu.injekt.Behavior
import com.ivianuu.injekt.BindingProvider
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.DuplicateStrategy
import com.ivianuu.injekt.Eager
import com.ivianuu.injekt.Factory
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.SideEffectBehavior
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.bind
import com.ivianuu.injekt.keyOf
import com.ivianuu.injekt.map

annotation class BindFragment {
    companion object : Behavior by (SideEffectBehavior {
        map<String, Fragment> {
            put(it.key.classifier.java.name, it.key as Key<out Fragment>)
        }
    } + Factory)
}

inline fun <reified T> ComponentBuilder.fragment(
    qualifier: Qualifier = Qualifier.None,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    noinline provider: BindingProvider<T>
) {
    fragment(
        key = keyOf(qualifier),
        behavior = behavior,
        duplicateStrategy = duplicateStrategy,
        provider = provider
    )
}

fun <T> ComponentBuilder.fragment(
    key: Key<T>,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    provider: BindingProvider<T>
) {
    bind(
        key = key,
        behavior = BindFragment + behavior,
        duplicateStrategy = duplicateStrategy,
        provider = provider
    )
}

inline fun <reified T> ComponentBuilder.bindFragment(
    qualifier: Qualifier = Qualifier.None
) {
    bindFragment(key = keyOf<T>(qualifier))
}

fun <T> ComponentBuilder.bindFragment(key: Key<T>) {
    alias(
        originalKey = key,
        aliasKey = key.copy(qualifier = BindFragmentDelegate),
        behavior = Eager,
        duplicateStrategy = DuplicateStrategy.Drop
    )
}

private object BindFragmentDelegate : Qualifier.Element

@Factory
private class InjektFragmentFactory(
    private val fragments: Map<String, Provider<Fragment>>
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment =
        fragments[className]?.invoke() ?: super.instantiate(classLoader, className)
}

@Module
private val FragmentInjectionModule = Module(ApplicationScope) {
    map<String, Fragment>()
    alias<InjektFragmentFactory, FragmentFactory>()
}
