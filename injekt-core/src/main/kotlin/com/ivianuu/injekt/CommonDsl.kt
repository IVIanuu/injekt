package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic

@Module
fun <S : T, T> alias(): Unit = injektIntrinsic()

@Module
fun <T> factory(definition: ProviderDefinition<T>): Unit = injektIntrinsic()

@Module
fun <T> instance(instance: T): Unit = injektIntrinsic()

@Module
fun <T> scoped(definition: ProviderDefinition<T>): Unit = injektIntrinsic()
