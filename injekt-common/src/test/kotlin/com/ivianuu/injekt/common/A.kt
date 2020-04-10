package com.ivianuu.injekt.common

import com.ivianuu.injekt.injekt
import org.junit.Test

class A {
    @Test
    fun init() {
        injekt { initializeEndpoint() }
    }
}
