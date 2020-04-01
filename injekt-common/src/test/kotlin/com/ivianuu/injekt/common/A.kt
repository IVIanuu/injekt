package com.ivianuu.injekt.common

import com.ivianuu.injekt.Injekt
import org.junit.Test

class A {
    @Test
    fun init() {
        Injekt { initializeEndpoint() }
    }
}