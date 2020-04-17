package com.ivianuu.injekt.sample

import com.ivianuu.injekt.Component
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.get
import com.ivianuu.injekt.parent

val GrandPa = Component("gp") {
    factory { 1L }
}

val Parent = Component("p") {
    factory { 2f }
    parent("gp", GrandPa)
    factory { 3 }
}

val Child = Component("c") {
    parent("p", Parent)
    factory { get<Int>().toString() }
}

fun main() {
    println("result ${Child.get<String>()} ${Child.get<Long>()}")
}