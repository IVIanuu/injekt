package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic

interface Context

inline fun <R> Context.runReader(block: () -> R): R = injektIntrinsic()

fun <T : Context> rootContext(vararg inputs: Any?): T = injektIntrinsic()

@Reader
fun <T : Context> childContext(vararg inputs: Any?): T = injektIntrinsic()

/**
 * Shorthand for
 * ```
 * interface MyContext : Context
 * rootContext<MyContext>(...).runReader { ... }
 * ```
 */
inline fun <R> runReader(vararg inputs: Any?, block: () -> R): R = injektIntrinsic()

/**
 * Shorthand for
 * ```
 * interface MyContext : Context
 * childContext<MyContext>(...).runReader { ... }
 * ```
 */
@Reader
inline fun <R> runChildReader(vararg inputs: Any?, block: () -> R): R = injektIntrinsic()
