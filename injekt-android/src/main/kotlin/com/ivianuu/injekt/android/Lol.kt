package com.ivianuu.injekt.android

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.synthetic.Factory

@Factory
class InjektFragmentFactory(
    private val fragments: Map<String, Provider<Fragment>>
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment =
        fragments[className]?.invoke() ?: super.instantiate(classLoader, className)
}