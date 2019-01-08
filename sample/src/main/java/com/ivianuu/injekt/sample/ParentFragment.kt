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
import com.ivianuu.injekt.android.fragment.FRAGMENT_SCOPE
import com.ivianuu.injekt.android.fragment.fragmentComponent
import com.ivianuu.injekt.annotations.Single
import com.ivianuu.injekt.inject

/**
 * @author Manuel Wrage (IVIanuu)
 */
class ParentFragment : Fragment(), InjektTrait {

    override val component by lazy { fragmentComponent(this) }

    private val appDependency by inject<AppDependency>()
    private val mainActivityDependency by inject<MainActivityDependency>()

    override fun onCreate(savedInstanceState: Bundle?) {
        d { "Injected app dependency $appDependency" }
        d { "Injected main activity dependency $mainActivityDependency" }

        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.child_container, ChildFragment())
                .commit()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_parent, container, false)
    }
}

@Single(scopeName = FRAGMENT_SCOPE)
class ParentFragmentDependency(
    val app: App,
    val mainActivity: MainActivity,
    val parentFragment: ParentFragment
)