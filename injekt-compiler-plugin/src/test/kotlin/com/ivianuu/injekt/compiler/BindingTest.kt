package com.facebook.buck.jvm.java.javax.com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.codegen
import com.ivianuu.injekt.compiler.invokeSingleFile
import org.junit.Test

class BindingTest {

    @Test
    fun testBindingDefinition() = codegen(
        """
        @Module 
        fun module() { 
            factory { Foo() }
            factory { Bar(get()) } 
            }
         
        fun invoke() = Component {
            module()
        }.get<Bar>()
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testClassBinding() = codegen(
        """
        @Factory 
        class MyClass(foo: Foo)
        
        fun invoke(): MyClass {
            Injekt.initializeEndpoint()
            return Component {
                factory { Foo() }
            }.get<MyClass>()
        }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testClassBindingWithScope() = codegen(
        """
        @ApplicationScoped 
        class MyClass(foo: Foo)
        
        fun invoke(): MyClass {
            Injekt.initializeEndpoint()
            return Component {
                factory { Foo() }
            }.get<MyClass>()
        }
    """
    ) {
        invokeSingleFile()
    }

}