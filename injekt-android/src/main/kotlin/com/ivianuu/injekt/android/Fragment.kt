package com.ivianuu.injekt.android

import androidx.fragment.app.Fragment
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentDsl
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.emptyParameters
import com.ivianuu.injekt.instance
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

fun ComponentDsl.fragment(instance: Fragment) {
    instance(instance, Key.SimpleKey(instance::class))
    alias(Key.SimpleKey(instance::class), keyOf<Fragment>())
    context(instance.requireContext(), ForFragment::class)
    lifecycleOwner(instance, ForFragment::class)
    savedStateRegistryOwner(instance, ForFragment::class)
    viewModelStoreOwner(instance, ForFragment::class)
}

inline fun <reified T> Fragment.getLazy(
    qualifier: KClass<*>? = null,
    crossinline parameters: () -> Parameters = { emptyParameters() }
): Lazy<T> = injektIntrinsic()

inline fun <T> Fragment.getLazy(
    key: Key<T>,
    crossinline parameters: () -> Parameters = { emptyParameters() }
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { fragmentComponent.get(key, parameters()) }

@Scope
annotation class FragmentScoped

@Qualifier
annotation class ForFragment
