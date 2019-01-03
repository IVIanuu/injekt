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
import com.ivianuu.injekt.annotations.Module
import com.ivianuu.injekt.annotations.Single
import com.ivianuu.injekt.common.instanceModule
import com.ivianuu.injekt.inject

/**
 * @author Manuel Wrage (IVIanuu)
 */
class ParentFragment : Fragment(), AppComponentTrait {

    private val appDependency by inject<AppDependency>()
    private val mainActivityDependency by inject<MainActivityDependency>()
    private val parentFragmentDependency by inject<ParentFragmentDependency>()

    override fun onCreate(savedInstanceState: Bundle?) {
        component.scopedModules(this, "PARENT_FRAGMENT",
            parentFragmentModule, instanceModule(this))

        appDependency
        mainActivityDependency
        parentFragmentDependency

        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .add(ChildFragment(), "child_fragment")
                .commit()
        }
    }

}

@Module private annotation class ParentFragmentModule

@Single @ParentFragmentModule
class ParentFragmentDependency(
    val app: App,
    val mainActivity: MainActivity,
    val parentFragment: ParentFragment
)