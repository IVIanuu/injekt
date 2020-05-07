package com.ivianuu.injekt.compiler

import org.junit.Test

class InlineModuleTest {

    @Test
    fun testSimpleInlineModule() = codegen(
        """
        @Module 
        fun callingModule() { 
            inlinedModule<Foo, Bar> { Bar(get()) } 
        }
        
        @Module
        inline fun <T : Any, S> inlinedModule(definition: ProviderDefinition<S>) {
            transient<T>()
            transient(definition)
            alias<T, Any>()
        }
    """
    )

}
