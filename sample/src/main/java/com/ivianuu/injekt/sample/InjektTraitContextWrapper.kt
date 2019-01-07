package com.ivianuu.injekt.sample

import android.content.Context
import android.content.ContextWrapper
import com.ivianuu.injekt.InjektTrait

/**
 * Used to allow fragment views to depend on the fragment component
 */
class InjektTraitContextWrapper(
    base: Context,
    injektTrait: InjektTrait
) : ContextWrapper(base), InjektTrait by injektTrait