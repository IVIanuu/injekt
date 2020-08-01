package com.ivianuu.injekt.lol

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.given
import com.ivianuu.injekt.initializeInjekt
import com.ivianuu.injekt.runReader

class Foo
class Bar(foo: Foo)

@Given
fun foo() = Foo()

@Given
fun bar() = Bar(given())

fun main() {
    initializeInjekt()
    runReader { given<Bar>() }
}
