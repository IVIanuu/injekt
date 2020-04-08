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
import com.ivianuu.injekt.BehaviorMarker
import com.ivianuu.injekt.Factory
import com.ivianuu.injekt.GenerateDslBuilder
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.ModuleMarker
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.QualifierMarker
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.android.synthetic.FragmentsMap
import com.ivianuu.injekt.common.map
import com.ivianuu.injekt.sideEffectBehavior
import com.ivianuu.injekt.synthetic.Factory

@GenerateDslBuilder
@BehaviorMarker
val BindFragment = sideEffectBehavior {
    map<String, Fragment>(mapQualifier = FragmentsMap) {
        put(it.key.classifier.java.name, it.key as Key<out Fragment>)
    }
} + Factory

@Factory
private class InjektFragmentFactory(
    @FragmentsMap private val fragments: Map<String, Provider<Fragment>>
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment =
        fragments[className]?.invoke() ?: super.instantiate(classLoader, className)
}

@ModuleMarker
private val FragmentInjectionModule = Module(ApplicationScope) {
    map<String, Fragment>(mapQualifier = FragmentsMap)
    alias<InjektFragmentFactory, FragmentFactory>()
}

@QualifierMarker
internal val FragmentsMap = Qualifier("FragmentsMap")
