package com.ivianuu.injekt.comparison.injekt

import com.ivianuu.injekt.Factory
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.createImplementation
import com.ivianuu.injekt.instance

interface Compon {
    val instance: String
}

@Module
fun a() {
    instance("hello world")
}

@Module
fun b() {
    a()
}

@Module
fun c() {
    b()
}

@Factory
fun factory(): Compon = createImplementation {
    c()
}