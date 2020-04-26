package com.facebook.buck.jvm.java.javax.com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.codegen
import com.ivianuu.injekt.compiler.invokeSingleFile
import org.junit.Test

class BindingTest {

    @Test
    fun testBindingDefinition() = codegen(
        """
        val MyModule = Module { 
            factory { Foo() }
            factory { Bar(get()) }
        }
         
        fun invoke() = Component<ApplicationScoped>(MyModule).get<Bar>()
    """
    ) {
        invokeSingleFile()
    }
}