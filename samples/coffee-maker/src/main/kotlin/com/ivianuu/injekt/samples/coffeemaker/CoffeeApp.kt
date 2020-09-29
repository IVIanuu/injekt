package com.ivianuu.injekt.samples.coffeemaker

import com.ivianuu.injekt.Module
import com.ivianuu.injekt.RootFactory
import com.ivianuu.injekt.rootFactory

fun main() {
    rootFactory<CoffeeComponentFactory>()(CoffeeModule())
}

interface CoffeeComponent

@RootFactory
typealias CoffeeComponentFactory = (CoffeeModule) -> CoffeeComponent

@Module
class CoffeeModule
