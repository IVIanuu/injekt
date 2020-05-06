package com.ivianuu.injekt.sample

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.ProviderDefinition
import com.ivianuu.injekt.Transient
import com.ivianuu.injekt.map
import com.ivianuu.injekt.transient
import kotlin.reflect.KClass

@Transient
class InjektFragmentFactory(
    private val fragments: Map<KClass<out Fragment>, @Provider () -> Fragment>
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        fragments[classLoader.loadClass(className).kotlin]?.invoke()?.let { return it }
        return super.instantiate(classLoader, className)
    }
}

@Module
inline fun <reified T : Fragment> fragment(
    noinline definition: ProviderDefinition<T>
) {
    transient(definition)
    map<KClass<out Fragment>, Fragment> {
        put<T>(T::class)
    }
}
