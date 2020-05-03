package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic
import kotlin.reflect.KFunction

@Declaration
fun <T> scope(): Unit = injektIntrinsic()

@Declaration
fun <T> dependency(dependency: T): Unit = injektIntrinsic()

@Declaration
fun <T : KFunction<*>> childFactory(factory: T): Unit = injektIntrinsic()

@Declaration
fun <S : T, T> alias(): Unit = injektIntrinsic()

@Declaration
fun <T> transient(definition: ProviderDefinition<T>): Unit = injektIntrinsic()

@Scope
annotation class Transient

@Declaration
fun <T> instance(instance: T): Unit = injektIntrinsic()

@Declaration
fun <T> scoped(definition: ProviderDefinition<T>): Unit = injektIntrinsic()
