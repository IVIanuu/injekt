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

import android.content.Context
import androidx.fragment.app.Fragment
import com.ivianuu.injekt.*
import com.ivianuu.injekt.android.fragment.fragmentComponent

/**
 * @author Manuel Wrage (IVIanuu)
 */
class ChildFragment : Fragment(), InjektTrait {

    override val component by lazy {
        fragmentComponent(this) {
            modules(childFragmentModule)
        }
    }

    private val appDependency by inject<AppDependency>()
    private val mainActivityDependency by inject<MainActivityDependency>()
    private val parentFragmentDependency by inject<ParentFragmentDependency>()
    private val childFragmentDependency by inject<ChildFragmentDependency>()

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        appDependency
        mainActivityDependency
        parentFragmentDependency
        childFragmentDependency
    }
}

val childFragmentModule = module("childFragmentModule") {
    single { ChildFragmentDependency(get(), get(), get(), get()) }
}

class ChildFragmentDependency(
    val app: App,
    val mainActivity: MainActivity,
    val parentFragment: ParentFragment,
    val childFragment: ChildFragment
)
