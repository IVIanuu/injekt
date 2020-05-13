package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic
import kotlin.reflect.KFunction

@Module
fun <T> parent(): Unit = injektIntrinsic()

@Module
fun <T : KFunction<*>> parentFactory(factory: T): Unit = injektIntrinsic()

@Module
fun <T> entryPoint(): Unit = injektIntrinsic()

@Module
fun <T> installIn(): Unit = injektIntrinsic()

