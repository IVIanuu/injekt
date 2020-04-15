package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Behavior
import com.ivianuu.injekt.BindingProvider
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.KeyOverload
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.bind

/* Used in codegen */
@KeyOverload
inline fun <T> BindingModule(
    key: Key<T>,
    behavior: Behavior,
    defaultScope: Scope,
    noinline provider: BindingProvider<T>
): Module {
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
