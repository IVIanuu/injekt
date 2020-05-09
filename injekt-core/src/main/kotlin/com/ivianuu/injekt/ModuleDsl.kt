package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic
import kotlin.reflect.KFunction

@Module
fun <T> scope(): Unit = injektIntrinsic()

@Module
fun <T> dependency(dependency: T): Unit = injektIntrinsic()

@Module
fun <T : KFunction<*>> childFactory(factory: T): Unit = injektIntrinsic()

@Module
fun <S : T, T> alias(): Unit = injektIntrinsic()

@Module
fun <T> transient(): Unit = injektIntrinsic()

@Module
inline fun <T> transient(definition: ProviderDefinition<T>): Unit = injektIntrinsic()

@Scope
annotation class Transient

@Module
fun <T> instance(instance: T): Unit = injektIntrinsic()

@Module
fun <T> scoped(): Unit = injektIntrinsic()

@Module
inline fun <T> scoped(definition: ProviderDefinition<T>): Unit = injektIntrinsic()
