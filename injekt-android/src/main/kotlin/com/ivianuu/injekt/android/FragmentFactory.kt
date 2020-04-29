package com.ivianuu.injekt.android

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.BindingDefinition
import com.ivianuu.injekt.Factory
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.internal.injektIntrinsic
import com.ivianuu.injekt.map
import kotlin.reflect.KClass

@Module
inline fun <reified T : Fragment> fragment(
    qualifier: KClass<*>? = null,
    bindingDefinition: BindingDefinition<T>
): Unit = injektIntrinsic()

@Module
inline fun <reified T : Fragment> fragment(
    qualifier: KClass<*>? = null,
    binding: Binding<T>
): Unit = injektIntrinsic()

@Module
inline fun <reified T : Fragment> fragment(
    key: Key<T>,
    bindingDefinition: BindingDefinition<T>
): Unit = injektIntrinsic()

@Module
fun <T : Fragment> fragment(
    key: Key<T>,
    binding: Binding<T>
) {
    factory(key, binding)
    bindFragment(key)
}

@Module
inline fun <reified T : Fragment> bindFragment(
    qualifier: KClass<*>? = null
): Unit = injektIntrinsic()

@Module
fun <T : Fragment> bindFragment(key: Key<T>) {
    map<String, Fragment>(Fragments::class) {
        put(key.classifier.java.name, key)
    }
}

@Module
fun fragmentInjection() {
    map<String, Fragment>(Fragments::class)
    alias<InjektFragmentFactory, FragmentFactory>()
}

@Qualifier
private annotation class Fragments

@Factory
private class InjektFragmentFactory(
    @Fragments private val fragments: Map<String, Provider<Fragment>>
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment =
        fragments[className]?.invoke() ?: super.instantiate(classLoader, className)
}
