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
import android.util.AttributeSet
import android.widget.FrameLayout
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.ScopeAnnotation
import com.ivianuu.injekt.Single
import com.ivianuu.injekt.android.ChildViewScope
import com.ivianuu.injekt.android.childViewComponent
import com.ivianuu.injekt.get

/**
 * @author Manuel Wrage (IVIanuu)
 */
class ChildView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), InjektTrait {

    override val component by lazy { childViewComponent() }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        d { "Injected app dependency ${get<AppDependency>()}" }
        d { "Injected main activity dependency ${get<MainActivityDependency>()}" }
        d { "Injected parent fragment dependency ${get<ParentFragmentDependency>()}" }
        d { "Injected child fragment dependency ${get<ChildFragmentDependency>()}" }
        d { "Injected parent view dependency ${get<ParentViewDependency>()}" }
        d { "Injected child view dependency ${get<ChildViewDependency>()}" }
    }
}

@Single @ChildViewScope
class ChildViewDependency(
    val app: App,
    val mainActivity: MainActivity,
    val parentFragment: ParentFragment,
    val childFragment: ChildFragment,
    val parentView: ParentView,
    val childView: ChildView
)