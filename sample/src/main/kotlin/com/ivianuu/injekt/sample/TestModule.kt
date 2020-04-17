package com.ivianuu.injekt.sample

import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentDsl
import com.ivianuu.injekt.Module

val MyComponent = Component("app") {
    foo()
}

@Module
fun ComponentDsl.foo() {
    factory { Foo("data") }
    factory { "cool" }
    bar()
}

@Module
fun ComponentDsl.bar() {
    factory { Bar(get(), "data") }
}

class Foo(data: String)

class Bar(foo: Foo, data: String)
