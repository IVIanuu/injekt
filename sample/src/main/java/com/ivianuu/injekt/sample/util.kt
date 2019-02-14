package com.ivianuu.injekt.sample

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.ivianuu.injekt.InjektTrait

inline fun Any.d(m: () -> String) {
    Log.d(javaClass.simpleName, m())
}

/**
 * Used to allow fragment views to depend on the fragment component
 */
class InjektTraitContextWrapper(
    base: Context,
    injektTrait: InjektTrait
) : ContextWrapper(base), InjektTrait by injektTrait