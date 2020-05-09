package com.ivianuu.injekt.compiler

import org.junit.Test

class InlineModuleTest {

    @Test
    fun testSimpleInlineModule() = codegen(
        """
        @Module 
        inline fun <T : Any, S> inlinedModule(definition: ProviderDefinition<S>) {
            transient<T>()
            inlinedModule2(definition)
        }
        
        @Module 
        inline fun <P> inlinedModule2(definition: ProviderDefinition<P>) {
            transient(definition)
        }
        
        @Factory
        fun factory(): Bar { 
            inlinedModule<Foo, Bar> { Bar(get()) }
            return createInstance()
        }
    """
    )

}
