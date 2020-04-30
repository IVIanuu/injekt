package com.ivianuu.injekt.compiler

import org.junit.Test

class ModuleTest {

    @Test
    fun test() = codegen(
        """
        @ChildFactory 
        fun testChildFactory(): TestComponent = createImplementation()
        
        @Module
        fun <T> other() {

        }

        @Module
        fun test() { 
            scope<TestScope>()
            dependency<Foo>(Foo())
            childFactory(::testChildFactory)
            other<String>()
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testSupportedChildFactory() = codegen(
        """
        @ChildFactory
        fun factory(): TestComponent {
            return createImplementation()
        }
        
        @Module
        fun module() {
            childFactory(::factory)
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testUnsupportedChildFactory() = codegen(
        """
        fun factory() {
        }
        
        @Module
        fun module() {
            childFactory(::factory)
        }
    """
    ) {
        assertCompileError("@ChildFactory")
    }


}