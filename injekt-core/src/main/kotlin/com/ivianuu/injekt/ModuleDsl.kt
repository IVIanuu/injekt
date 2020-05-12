package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic
import kotlin.reflect.KFunction

fun <T> scope(): Unit = injektIntrinsic()

fun <T> dependency(dependency: T): Unit = injektIntrinsic()

fun <T : KFunction<*>> childFactory(factory: T): Unit = injektIntrinsic()

fun <S : T, T> alias(): Unit = injektIntrinsic()

fun <T> transient(): Unit = injektIntrinsic()

inline fun <T> transient(definition: ProviderDefinition<T>): Unit = injektIntrinsic()

@Scope
annotation class Transient

fun <T> instance(instance: T): Unit = injektIntrinsic()

fun <T> scoped(): Unit = injektIntrinsic()

inline fun <T> scoped(definition: ProviderDefinition<T>): Unit = injektIntrinsic()
