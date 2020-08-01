package com.ivianuu.injekt.lol

import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.runReader

class Foo

fun runApplicationReader(block: @Reader () -> Foo): Foo {
    return runReader(Foo()) { block() }
}
