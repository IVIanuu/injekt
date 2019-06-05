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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.Single
import com.ivianuu.injekt.android.ChildFragmentScope

import com.ivianuu.injekt.component
import com.ivianuu.injekt.get

class ChildFragment : Fragment(), InjektTrait {

    override val component by lazy { component() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        d { "Injected app dependency ${get<AppDependency>()}" }
        d { "Injected main activity dependency ${get<MainActivityDependency>()}" }
        d { "Injected parent fragment dependency ${get<ParentFragmentDependency>()}" }
        d { "Injected child fragment dependency ${get<ChildFragmentDependency>()}" }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val injektInflater = inflater.cloneInContext(
            InjektTraitContextWrapper(requireContext(), this)
        )
        return injektInflater.inflate(R.layout.fragment_child, container, false)
    }
}

@Single @ChildFragmentScope
class ChildFragmentDependency(
    val app: App,
    val mainActivity: MainActivity,
    val parentFragment: ParentFragment,
    val childFragment: ChildFragment
)