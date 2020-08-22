package com.ivianuu.injekt.lol

import com.ivianuu.injekt.ApplicationContext
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.InitializeInjekt
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.given
import com.ivianuu.injekt.rootContext
import com.ivianuu.injekt.runReader

class Foo
class Bar(foo: Foo)

@Given
fun foo() = Foo()

@Reader
fun <T> provide() = given<T>()

@InitializeInjekt
fun invoke(): Foo {
    return rootContext<ApplicationContext>().runReader { provide() }
}
