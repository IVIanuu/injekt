/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.fir.*

data class SourcePosition(val filePath: String, val startOffset: Int, val endOffset: Int)

const val INJECTIONS_OCCURRED_IN_FILE_KEY = "injections_occurred_in_file"
const val INJECTION_RESULT_KEY = "injection_result"

class InjektCache {
  lateinit var session: FirSession

  @PublishedApi internal val map = mutableMapOf<Any, Any>()

  fun <K, V : Any> cachedOrNull(kind: String, key: K): V? = map[kind to key] as? V

  inline fun <K, V : Any> cached(
    kind: String,
    key: K,
    computation: () -> V
  ): V = map.getOrPut(kind to key) { computation() } as V
}
