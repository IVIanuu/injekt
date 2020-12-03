package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Module

// Global generic modules??
// Lazy resolve global modules

class TupledModule1<A>(@Module val a: A)

class TupledModule2<A, B>(
    @Module val a: A,
    @Module val b: B
)

class CounterModule {
    @Binding
    fun lol() = ""
}

@Module
val CounterKeyModule = TupledModule1(CounterModule())
