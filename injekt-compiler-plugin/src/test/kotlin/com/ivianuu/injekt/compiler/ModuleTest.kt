package com.ivianuu.injekt.compiler

import junit.framework.Assert.assertEquals
import org.junit.Test

class ModuleTest {

    @Test
    fun testImplicitModule() = codegen(
        """
        @ApplicationScoped
        val MyModule = Module {
            factory { "hello world" }
        }
        
        fun invoke(): String {
            Injekt.initializeEndpoint()
            return Component().get<String>()
        }
    """
    ) {
        assertEquals("hello world", invokeSingleFile())
    }

}