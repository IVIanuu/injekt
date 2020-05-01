package com.facebook.buck.jvm.java.javax.com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.codegen
import com.ivianuu.injekt.compiler.invokeSingleFile
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
            transient { "hello world" }
            //instance("hello world") 
        }
        
        fun invoke() = create().helloWorld
    """
    ) {
        assertEquals("hello world", invokeSingleFile())
    }


}