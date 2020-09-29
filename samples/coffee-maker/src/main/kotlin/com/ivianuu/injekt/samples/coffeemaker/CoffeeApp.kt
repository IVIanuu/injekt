package com.ivianuu.injekt.samples.coffeemaker

import com.ivianuu.injekt.Component

fun main() {
    val context = rootContext<ApplicationContainer>()
    context.given<brewCoffee>()("")
}

@Component
interface CoffeeComponent {
    @Component.Factory
    interface Factory {
        fun create(): CoffeeComponent
    }
}
