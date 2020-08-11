package com.ivianuu.injekt.lol

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.given

class Foo

class Bar(val foo: Foo)

@Given
fun foo() = Foo()

@Reader
class FooFactory {
    fun getFoo() = given<Foo>()
}
