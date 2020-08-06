package com.ivianuu.injekt.lol

import com.ivianuu.injekt.Effect
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.InitializeInjekt
import com.ivianuu.injekt.given
import com.ivianuu.injekt.runReader

@InitializeInjekt
interface InjektInitializer

class Foo

class Bar(val foo: Foo)

@Effect
annotation class Effect1 {
    companion object {
        @Given
        fun <T> bind() = given<T>().toString()
    }
}

@Effect
annotation class Effect2 {
    companion object {
        @Given
        fun <T : Any> bind(): Any = given<T>()
    }
}

@Effect1
@Effect2
@Given
class Dep

fun invoke() {
    runReader {
        given<Dep>()
        given<String>()
        given<Any>()
    }
}
