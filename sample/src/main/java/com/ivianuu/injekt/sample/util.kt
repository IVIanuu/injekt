package com.ivianuu.injekt.sample

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.annotations.Name
import com.ivianuu.injekt.annotations.Param
import com.ivianuu.injekt.annotations.Single


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

@Single("name", "scope", true, true)
class DummyDep(
    @Name("named") dummyDep: DummyDep,
    @Param parameterized: Boolean,
    lazyDep: Lazy<DummyDep>,
    providerDep: Provider<DummyDep>,
    normal: DummyDep,
    @Param parameterized2: Int
)