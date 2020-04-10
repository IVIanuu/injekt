package com.ivianuu.injekt

import org.junit.Test

class A {
    @Test
    fun init() {
        injekt { initializeEndpoint() }
    }
}
