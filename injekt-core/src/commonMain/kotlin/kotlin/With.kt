/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlin

import com.ivianuu.injekt.Context

inline fun <A, B, R> with(@Context a: A, @Context b: B, block: context(A, B) () -> R) = block()

inline fun <A, B, C, R> with(@Context a: A, @Context b: B, @Context c: C, block: context(A, B, C) () -> R) = block()

inline fun <A, B, C, D, R> with(@Context a: A, @Context b: B, @Context c: C, @Context d: D, block: context(A, B, C, D) () -> R) = block()

inline fun <A, B, C, D, E, R> with(@Context a: A, @Context b: B, @Context c: C, @Context d: D, @Context e: E, block: context(A, B, C, D, E) () -> R) = block()
