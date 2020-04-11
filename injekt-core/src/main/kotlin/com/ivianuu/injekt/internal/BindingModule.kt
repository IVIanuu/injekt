package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Behavior
import com.ivianuu.injekt.Behavior.None.foldInBehavior
import com.ivianuu.injekt.BindingProvider
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.KeyOverload
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Scope

/* Used in codegen */
@KeyOverload
fun <T> BindingModule(
    key: Key<T>,
    behavior: Behavior,
    defaultScope: Scope,
    provider: BindingProvider<T>
): Module {
    val scope = foldInBehavior(null) { acc: Scope?, element ->
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
