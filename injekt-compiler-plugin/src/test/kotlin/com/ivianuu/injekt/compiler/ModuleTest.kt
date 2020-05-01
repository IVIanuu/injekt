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

    @Test
    fun testQualifiedExpression() = codegen(
        """
        @Module
        fun module() {
            @TestQualifier1
            set<String>()
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testMap() = codegen(
        """
        @Module
        fun module() {
            map<String, String> {
                put<@TestQualifier1 String>("hello")
            }
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testSet() = codegen(
        """
        @Module
        fun module() {
            set<String> {
                add<@TestQualifier1 String>()
            }
        }
    """
    ) {
        assertOk()
    }

}
