package com.ivianuu.injekt.lol

import com.ivianuu.injekt.Context
import com.ivianuu.injekt.Effect
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.InitializeInjekt
import com.ivianuu.injekt.childContext
import com.ivianuu.injekt.given
import com.ivianuu.injekt.rootContext
import com.ivianuu.injekt.runReader

class Foo
class Bar(foo: Foo)

@Context
interface TestChildContext
@Context
interface TestParentContext

@Effect
annotation class GivenFooFactory {
    companion object {
        @Given
        fun <T : () -> Foo> invoke(): FooFactoryMarker = given<T>()
    }
}

typealias FooFactoryMarker = () -> Foo

@GivenFooFactory
fun FooFactoryImpl() = childContext<TestChildContext>().runReader { given<Foo>() }

@InitializeInjekt
fun main() {
    val a = Foo()
    val b = rootContext<TestParentContext>(a).runReader {
        given<FooFactoryMarker>()()
    }

    check(a === b)
}
