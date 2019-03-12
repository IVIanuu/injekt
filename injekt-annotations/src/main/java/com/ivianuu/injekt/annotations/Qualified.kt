package com.ivianuu.injekt.annotations

import com.ivianuu.injekt.Qualifier
import kotlin.reflect.KClass

/**
 * @author Manuel Wrage (IVIanuu)
 */
annotation class Qualified(val value: KClass<out Qualifier>)