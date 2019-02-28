package com.ivianuu.injekt.sample

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.android.VIEW_SCOPE
import com.ivianuu.injekt.android.viewComponent
import com.ivianuu.injekt.get

import com.ivianuu.injekt.inject
import com.ivianuu.injekt.module
import com.ivianuu.injekt.modules
import com.ivianuu.injekt.single

/**
 * @author Manuel Wrage (IVIanuu)
 */
class ParentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), InjektTrait {

    override val component by lazy {
        viewComponent {
            modules(parentViewModule)
        }
    }

    private val appDependency by inject<AppDependency>()
    private val mainActivityDependency by inject<MainActivityDependency>()
    private val parentFragmentDependency by inject<ParentFragmentDependency>()
    private val childFragmentDependency by inject<ChildFragmentDependency>()
    private val parentViewDependency by inject<ParentViewDependency>()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        d { "Injected app dependency $appDependency" }
        d { "Injected main activity dependency $mainActivityDependency" }
        d { "Injected parent fragment dependency $parentFragmentDependency" }
        d { "Injected child fragment dependency $childFragmentDependency" }
        d { "Injected parent view dependency $parentViewDependency" }
    }

}

val parentViewModule = module {
    single(scopeName = VIEW_SCOPE) {
        ParentViewDependency(get(), get(), get(), get(), get())
    }
}

class ParentViewDependency(
    val app: App,
    val mainActivity: MainActivity,
    val parentFragment: ParentFragment,
    val childFragment: ChildFragment,
    val parentView: ParentView
)