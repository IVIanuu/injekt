package com.ivianuu.injekt.sample

import android.util.Log

/**
 * @author Manuel Wrage (IVIanuu)
 */
interface Dependency

inline fun Any.d(m: () -> String) {
    Log.d(javaClass.simpleName, m())
}