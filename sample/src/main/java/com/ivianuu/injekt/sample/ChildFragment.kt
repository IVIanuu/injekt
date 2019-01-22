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
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.android.ACTIVITY_SCOPE
import com.ivianuu.injekt.android.fragment.CHILD_FRAGMENT_SCOPE
import com.ivianuu.injekt.android.fragment.childFragmentComponent
import com.ivianuu.injekt.annotations.Name
import com.ivianuu.injekt.annotations.Single
import com.ivianuu.injekt.inject

/**
 * @author Manuel Wrage (IVIanuu)
 */
class ChildFragment : Fragment(), InjektTrait {

    override val component by lazy { childFragmentComponent(this) }

    private val appDependency by inject<AppDependency>()
    private val mainActivityDependency by inject<MainActivityDependency>()
    private val parentFragmentDependency by inject<ParentFragmentDependency>()
    private val childFragmentDependency by inject<ChildFragmentDependency>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appDependency
        mainActivityDependency
        parentFragmentDependency
        childFragmentDependency

        d { "Injected app dependency $appDependency" }
        d { "Injected main activity dependency $mainActivityDependency" }
        d { "Injected parent fragment dependency $parentFragmentDependency" }
        d { "Injected child fragment dependency $childFragmentDependency" }
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

@Single(scopeName = CHILD_FRAGMENT_SCOPE)
class ChildFragmentDependency(
    val app: App,
    val mainActivity: MainActivity,
    val parentFragment: ParentFragment,
    val childFragment: ChildFragment,
    @Name(ACTIVITY_SCOPE) val activityComponent: Component
) {

    init {
        d { "activity component -> $activityComponent, ${activityComponent.getScopeNames()}, ${activityComponent.getBindings()}" }
    }

}