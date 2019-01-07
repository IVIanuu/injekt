package com.ivianuu.injekt.sample

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.android.viewComponent
import com.ivianuu.injekt.get
import com.ivianuu.injekt.inject
import com.ivianuu.injekt.module
import com.ivianuu.injekt.modules
import com.ivianuu.injekt.multibinding.bindIntoMap
import com.ivianuu.injekt.multibinding.injectMap
import com.ivianuu.injekt.single
import kotlin.reflect.KClass

/**
 * @author Manuel Wrage (IVIanuu)
 */
class ParentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), InjektTrait {

    override val component by lazy {
        viewComponent(this) {
            modules(parentViewModule)
        }
    }

    private val appDependency by inject<AppDependency>()
    private val mainActivityDependency by inject<MainActivityDependency>()
    private val parentFragmentDependency by inject<ParentFragmentDependency>()
    private val childFragmentDependency by inject<ChildFragmentDependency>()
    private val parentViewDependency by inject<ParentViewDependency>()

    private val dependencies by injectMap<KClass<out Dependency>, Dependency>(DEPS)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        d { "Injected app dependency $appDependency" }
        d { "Injected main activity dependency $mainActivityDependency" }
        d { "Injected parent fragment dependency $parentFragmentDependency" }
        d { "Injected child fragment dependency $childFragmentDependency" }
        d { "Injected parent view dependency $parentViewDependency" }
        d { "All dependencies $dependencies" }
    }

}

class ParentViewDependency(
    val app: App,
    val mainActivity: MainActivity,
    val parentFragment: ParentFragment,
    val childFragment: ChildFragment,
    val parentView: ParentView
) : Dependency

val parentViewModule = module {
    single {
        ParentViewDependency(
            get(),
            get(),
            get(),
            get(),
            get()
        )
    } bindIntoMap (DEPS to ParentViewDependency::class)
}