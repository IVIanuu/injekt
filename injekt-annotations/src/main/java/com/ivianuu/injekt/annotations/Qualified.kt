package com.ivianuu.injekt.annotations

import com.ivianuu.injekt.Qualifier
import kotlin.reflect.KClass

/**
 * @author Manuel Wrage (IVIanuu)
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Qualified(val qualifier: KClass<out Qualifier>)