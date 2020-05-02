package com.ivianuu.injekt.compiler

import junit.framework.Assert.assertEquals
import org.junit.Test

class Testt {

    @Test
    fun lol() = codegen(
        """
        interface MyComponent {
            val helloWorld: String
        }
        
        @Factory 
        fun create(): MyComponent = createImplementation {
            instance(0)
            transient { 0L }
            scoped { get<Int>(); get<Long>(); "hello world" }
        }
        
        fun invoke() = create().helloWorld
    """
    ) {
        assertEquals("hello world", invokeSingleFile())
    }

    @Test
    fun lolo() = codegen(
        """
        interface MyComponent {
            val helloWorld: TestDep
        }
        
        @Transient
        class TestDep
        
        @Factory 
        fun create(): MyComponent = createImplementation()
        
        fun invoke() = create().helloWorld
    """
    ) {
        invokeSingleFile()
    }

}