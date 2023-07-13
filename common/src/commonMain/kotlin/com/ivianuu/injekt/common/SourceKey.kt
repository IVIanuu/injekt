/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

/**
 * A key which is unique for each root call site
 */
@JvmInline value class SourceKey(val value: String)

/**
 * Returns the [SourceKey] at this call site
 */
context(SourceKey) inline fun sourceKey(): SourceKey = this@SourceKey
