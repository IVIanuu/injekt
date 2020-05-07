package com.ivianuu.injekt.compiler

import org.junit.Test

class FactoryInstanceTest {

    @Test
    fun testCreateInstance() = codegen(
        """
        @Factory
        fun createBar(): Bar { 
            transient<Foo>()
            transient<Bar>()
            return createInstance()
        }
        
        fun invoke() = createBar()
    """
    ) {
        invokeSingleFile()
    }

}