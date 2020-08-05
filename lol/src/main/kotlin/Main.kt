package com.ivianuu.injekt.lol

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.InitializeInjekt
import com.ivianuu.injekt.given
import com.ivianuu.injekt.runChildReader
import com.ivianuu.injekt.runReader

@InitializeInjekt
interface InjektInitializer

class Foo

class Bar(val foo: Foo)

@Given
fun foo() = Foo()

@Given
fun bar() = Bar(given())

fun invoke(foo: Foo): Foo {
    return runReader {
        runChildReader(foo) {
            given<Bar>().foo
        }
    }
}
