package com.ivianuu.injekt.samples.comparison.injekt

import com.ivianuu.injekt.RootFactory
import com.ivianuu.injekt.samples.comparison.fibonacci.Fib8

interface InjektComponent {
    val fib8: Fib8
}

@RootFactory
typealias InjektComponentFactory = () -> InjektComponent
