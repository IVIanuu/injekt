package com.ivianuu.injekt.android

import androidx.fragment.app.Fragment
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.emptyParameters
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.instanceKeyOf
import com.ivianuu.injekt.internal.injektIntrinsic
import com.ivianuu.injekt.keyOf
import com.ivianuu.injekt.plus
import kotlin.reflect.KClass

val Fragment.fragmentComponent: Component
    get() = lifecycle.component {
        requireActivity().activityComponent.plus<FragmentScoped> {
            fragment(this@fragmentComponent)
        }
    }

@Module
fun fragment(instance: Fragment) {
    val instanceKey = instanceKeyOf(instance)
    instance(instance, instanceKey)
    alias(instanceKey, keyOf())
    factory(ForFragment::class) { instance.requireContext() }
    lifecycleOwner(instanceKey, ForFragment::class)
    factory(ForFragment::class) { instance.resources }
    savedStateRegistryOwner(instanceKey, ForFragment::class)
    viewModelStoreOwner(instanceKey, ForFragment::class)
}

inline fun <reified T> Fragment.inject(
    qualifier: KClass<*>? = null,
    crossinline parameters: () -> Parameters = { emptyParameters() }
): Lazy<T> = injektIntrinsic()

inline fun <T> Fragment.inject(
    key: Key<T>,
    crossinline parameters: () -> Parameters = { emptyParameters() }
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { fragmentComponent.get(key, parameters()) }

@Scope
annotation class FragmentScoped

@Qualifier
annotation class ForFragment
