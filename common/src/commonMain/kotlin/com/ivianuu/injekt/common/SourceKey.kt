package com.ivianuu.injekt.common

import com.ivianuu.injekt.*
import kotlin.jvm.*

/**
 * A key which is unique for each root call site
 */
@JvmInline value class SourceKey(val value: String)

/**
 * Returns the [SourceKey] at this call site
 */
inline fun sourceKey(x: SourceKey = inject): SourceKey = x
