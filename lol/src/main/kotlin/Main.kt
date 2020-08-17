package com.ivianuu.injekt.lol

import com.ivianuu.injekt.Context
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.InitializeInjekt
import com.ivianuu.injekt.given
import com.ivianuu.injekt.rootContext
import com.ivianuu.injekt.runReader

class Foo

class Bar(val foo: Foo)

@Context
interface SimpleContext

@Given
fun foo() = Foo()

@Given
fun bar() = Bar(given())

@InitializeInjekt
fun main() {
    rootContext<SimpleContext>().runReader { given<Bar>() }
}
