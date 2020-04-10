package com.ivianuu.injekt

import org.junit.Test

// todo find a workaround
class A {

    @Test
    fun init() {
        injekt {
            initializeEndpoint()
            logger = PrintLogger()
        }
    }

}
