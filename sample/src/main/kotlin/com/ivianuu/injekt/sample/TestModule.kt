package com.ivianuu.injekt.sample

import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentDsl
import com.ivianuu.injekt.Module

val My2Component = Component("app") {
    foo("dataString")
}

@Module
fun ComponentDsl.foo(data: String) {
    factory { Foo(data) }
    bar(data)
}

@Module
fun ComponentDsl.bar(data: String) {
    factory { Bar(get(), data) }
}

class Foo(data: String)

class Bar(foo: Foo, data: String)
