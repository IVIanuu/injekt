package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Behavior
import com.ivianuu.injekt.BindingProvider
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.ModuleImpl
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.bind
import com.ivianuu.injekt.keyOf

/* Used in codegen */
inline fun <reified T> BindingModule(
    qualifier: Qualifier = Qualifier.None,
    behavior: Behavior,
    defaultScope: Scope,
    noinline provider: BindingProvider<T>
): ModuleImpl = BindingModule(keyOf(qualifier), behavior, defaultScope, provider)

inline fun <T> BindingModule(
    key: Key<T>,
    behavior: Behavior,
    defaultScope: Scope,
    noinline provider: BindingProvider<T>
): ModuleImpl {
    val scope = behavior.foldInBehavior(null) { acc: Scope?, element ->
        acc ?: element as? Scope
    } ?: defaultScope

    return Module(scope) {
        bind(
            key = key,
            behavior = behavior,
            provider = provider
        )
    }
}
