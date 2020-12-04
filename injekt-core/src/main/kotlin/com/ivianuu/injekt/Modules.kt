package com.ivianuu.injekt

inline class alias<A : B, B>(private val unused: Any = Unit) {
    @Binding
    inline val A.bind: B get() = this
}
