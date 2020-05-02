package com.facebook.buck.jvm.java.javax.com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.codegen
import com.ivianuu.injekt.compiler.invokeSingleFile
import junit.framework.Assert.assertSame
import org.junit.Test

class ScopedTest {

    @Test
    fun testDslScoped() = codegen(
        """
        interface SimpleComponent {
           val bar: Bar
        } 
        
        @Factory
        fun create() = createImplementation<SimpleComponent> {
            transient { Foo() }
            scoped { Bar(get()) }
        }
        
        val component = create()
        fun invoke() = component.bar
    """
    ) {
        TODO()
        assertSame(
            invokeSingleFile(),
            invokeSingleFile()
        )
    }

}