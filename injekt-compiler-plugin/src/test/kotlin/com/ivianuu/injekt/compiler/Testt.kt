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
            scoped { get<Int>(); get<Long>(); "hello world" }
            transient { 0L }
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

    @Test
    fun fib() = codegen(
        """
        interface MyComponent {
            val fib4: Fib4
        }
        
        @Transient class Fib1
        @Transient class Fib2
        @Transient class Fib3(val m1: Fib2, val m2: Fib1)
        @Transient class Fib4(val m1: Fib3, val m2: Fib2)

        @Factory 
        fun create(): MyComponent = createImplementation()
         """
    ) {
        assertOk()
    }

}