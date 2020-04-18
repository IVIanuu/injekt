package com.ivianuu.injekt.sample

import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.factory

inline fun <reified A, reified B> CreateComponent(a: A, b: B) = Component("key") {
    factory { a }
    factory { b }
}

class component<A, B>(a: A, b: B) {
    val module = module<A, B>(a, b)
}

class module<A, B>(
    val a: A,
    val b: B
) {
    class provider_0<A, B>(val module: module<A, B>) : Provider<A> {
        override fun invoke(): A = module.a
    }

    class provider_1<A, B>(val module: module<A, B>) : Provider<B> {
        override fun invoke(): B = module.b
    }
}