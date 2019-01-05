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

package com.ivianuu.injekt.sample

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.ivianuu.injekt.*
import com.ivianuu.injekt.android.fragment.fragmentComponent
import com.ivianuu.injekt.multibinding.bindIntoMap
import com.ivianuu.injekt.multibinding.injectMap
import kotlin.reflect.KClass

/**
 * @author Manuel Wrage (IVIanuu)
 */
class ParentFragment : Fragment(), InjektTrait {

    override val component by lazy {
        fragmentComponent(this) {
            modules(parentFragmentModule)
        }
    }

    private val appDependency by inject<AppDependency>()
    private val mainActivityDependency by inject<MainActivityDependency>()
    private val parentFragmentDependency by inject<ParentFragmentDependency>()

    private val dependencies by injectMap<KClass<out Dependency>, Dependency>(DEPS)

    override fun onCreate(savedInstanceState: Bundle?) {
        d { "Injected app dependency $appDependency" }
        d { "Injected main activity dependency $mainActivityDependency" }
        d { "Injected parent fragment dependency $parentFragmentDependency" }
        d { "All dependencies $dependencies" }

        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .add(ChildFragment(), "child_fragment")
                .commit()
        }
    }

}

class ParentFragmentDependency(
    val app: App,
    val mainActivity: MainActivity,
    val parentFragment: ParentFragment
) : Dependency

val parentFragmentModule = module {
    single {
        ParentFragmentDependency(
            get(),
            get(),
            get()
        )
    } bindIntoMap (DEPS to ParentFragmentDependency::class)
}