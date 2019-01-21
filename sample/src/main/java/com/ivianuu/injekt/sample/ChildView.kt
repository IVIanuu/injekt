package com.ivianuu.injekt.sample

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.android.CHILD_VIEW_SCOPE
import com.ivianuu.injekt.android.childViewComponent
import com.ivianuu.injekt.annotations.Single
import com.ivianuu.injekt.inject

/**
 * @author Manuel Wrage (IVIanuu)
 */
class ChildView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), InjektTrait {

    override val component by lazy { childViewComponent(this) }

    private val appDependency by inject<AppDependency>()
    private val mainActivityDependency by inject<MainActivityDependency>()
    private val parentFragmentDependency by inject<ParentFragmentDependency>()
    private val childFragmentDependency by inject<ChildFragmentDependency>()
    private val parentViewDependency by inject<ParentViewDependency>()
    private val childViewDependency by inject<ChildViewDependency>()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        d { "Injected app dependency $appDependency" }
        d { "Injected main activity dependency $mainActivityDependency" }
        d { "Injected parent fragment dependency $parentFragmentDependency" }
        d { "Injected child fragment dependency $childFragmentDependency" }
        d { "Injected parent view dependency $parentViewDependency" }
        d { "Injected child view dependency $childViewDependency" }
    }
}

@Single(CHILD_VIEW_SCOPE)
class ChildViewDependency(
    val app: App,
    val mainActivity: MainActivity,
    val parentFragment: ParentFragment,
    val childFragment: ChildFragment,
    val parentView: ParentView,
    val childView: ChildView
)