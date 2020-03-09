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
import com.ivianuu.injekt.Behavior
import com.ivianuu.injekt.BindingContext
import com.ivianuu.injekt.BindingProvider
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.DuplicateStrategy
import com.ivianuu.injekt.Factory
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.QualifierMarker
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.withBinding

fun FragmentFactoryComponent() = Component {
    map<String, Fragment>(mapQualifiers = FragmentsMap)
    alias<InjektFragmentFactory, FragmentFactory>()
}

inline fun <reified T : Fragment> ComponentBuilder.fragment(
    qualifier: Qualifier = Qualifier.None,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    noinline provider: BindingProvider<T>
) {
    factory(qualifier, behavior, duplicateStrategy, provider)
        .bindFragment()
}

inline fun <reified T : Fragment> ComponentBuilder.bindFragment(
    qualifier: Qualifier = Qualifier.None
) {
    withBinding<T>(qualifier) { bindFragment() }
}

inline fun <reified T : Fragment> BindingContext<T>.bindFragment(): BindingContext<T> {
    intoMap<String, Fragment>(
        entryKey = T::class.java.name,
        mapQualifier = FragmentsMap
    )
    return this
}

@Factory
class InjektFragmentFactory(
    @FragmentsMap private val fragments: Map<String, Provider<Fragment>>
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment =
        fragments[className]?.invoke() ?: super.instantiate(classLoader, className)
}

@QualifierMarker
annotation class FragmentsMap {
    companion object : Qualifier.Element
}
