package com.ivianuu.injekt.sample

import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentDsl
import com.ivianuu.injekt.Module

fun ComponentWithParams(
    userId: String
) = Component("parameterized") {
    factory { userId }
}

val My2Component = Component("app") {
    parent("parent", ParentComponent)
    foo("dataStringfff")
}

val ParentComponent = Component("parent") {
    factory { 0 }
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
