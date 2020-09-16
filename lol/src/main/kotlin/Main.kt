package com.ivianuu.injekt.lol

import com.ivianuu.injekt.Context
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.InitializeInjekt
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.given
import com.ivianuu.injekt.rootContext
import com.ivianuu.injekt.runReader

class Foo
class Bar(foo: Foo)

@Context
interface TestContext

@Given
fun foo() = Foo()

@Reader
fun <T> provide() = given<T>()

@InitializeInjekt
fun invoke(): Foo {
    return rootContext<TestContext>().runReader { provide() }
}
