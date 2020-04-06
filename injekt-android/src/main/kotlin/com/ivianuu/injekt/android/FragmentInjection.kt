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
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.DuplicateStrategy
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.KeyOverload
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.QualifierMarker
import com.ivianuu.injekt.Tag
import com.ivianuu.injekt.TagMarker
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.android.synthetic.FragmentsMap
import com.ivianuu.injekt.common.map
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.sideEffectTag
import com.ivianuu.injekt.synthetic.ApplicationScope
import com.ivianuu.injekt.synthetic.Factory

@TagMarker
val BindFragment = sideEffectTag("BindFragment") {
    bindFragmentIntoMap(it.key as Key<out Fragment>)
}

@KeyOverload
inline fun <T : Fragment> ComponentBuilder.fragment(
    key: Key<T>,
    tag: Tag = Tag.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    crossinline provider: Component.(Parameters) -> T
) {
    factory(key, tag, duplicateStrategy, provider)
    bindFragmentIntoMap(fragmentKey = key)
}

@KeyOverload
fun <T : Fragment> ComponentBuilder.bindFragmentIntoMap(fragmentKey: Key<T>) {
    map<String, Fragment>(mapQualifier = FragmentsMap) {
        put(fragmentKey.classifier.java.name, fragmentKey)
    }
}

@Factory
class InjektFragmentFactory(
    @FragmentsMap private val fragments: Map<String, Provider<Fragment>>
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment =
        fragments[className]?.invoke() ?: super.instantiate(classLoader, className)
}

@ApplicationScope
@Module
private fun ComponentBuilder.fragmentInjectionModule() {
    map<String, Fragment>(mapQualifier = FragmentsMap)
    alias<InjektFragmentFactory, FragmentFactory>()
}

@QualifierMarker
internal val FragmentsMap = Qualifier("FragmentsMap")
