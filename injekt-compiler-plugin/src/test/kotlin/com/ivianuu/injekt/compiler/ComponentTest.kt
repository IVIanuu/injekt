package com.ivianuu.injekt.compiler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class ComponentTest {

    @Test
    fun testSimple() = codegen(
        """
        val TestComponent = Component("c") {
            factory { "test" }
        }
        
        fun invoke() = TestComponent.get<String>()
    """,
    ) {
        assertEquals("test", invokeSingleFile())
    }

    @Test
    fun testWithMultipleDeps() = codegen(
        """
        val TestComponent = Component("c") {
            factory { "test" }
            factory { 2 }
        }
        fun invoke() = TestComponent.get<String>() to TestComponent.get<Int>()
    """
    ) {
        assertEquals("test" to 2, invokeSingleFile())
    }

    @Test
    fun testWithModule() = codegen(
        """
        @Module
        fun module() {
            factory { "test" }
        }
        
        val TestComponent = Component("c") { module() }
        
        fun invoke() = TestComponent.get<String>()
    """
    ) {
        assertEquals("test", invokeSingleFile())
    }

    @Test
    fun testWithNestedModule() = codegen(
        """
        @Module
        fun a() {
            factory { "test" }
            b()
        }
        
        @Module
        fun b() { 
            factory { 2 }
        }
        
        val TestComponent = Component("c") {
            a()
        }
        
        fun invoke() = TestComponent.get<String>() to TestComponent.get<Int>()
    """
    ) {
        assertEquals("test" to 2, invokeSingleFile())
    }

    @Test
    fun testDuplicatedBindingFails() = codegen(
        """
        val TestComponent = Component("c") {
            factory { "a" }
            factory { "b" }
        }
    """
    ) {
        assertInternalError("duplicate")
    }

    @Test
    fun testNestedDuplicatedBindingFails() = codegen(
        """
        val componentA = Component("a") { 
            factory { "a" } 
        } 
        val componentB = Component("b") { 
            parent("a", componentA) 
            factory { "b" }
        }
    """
    ) {
        assertInternalError("duplicate")
    }

    @Test
    fun testParentsWithDuplicatedBindingFails() = codegen(
        """
        val componentA = Component("a") { 
            factory { "a" }
        } 
        val componentB = Component("b") { 
            factory { "b" }
        }
        val componentC = Component("c") {
            parent("a", componentA)
            parent("b", componentB)
        }
    """
    ) {
        assertInternalError("duplicated")
    }

    @Test
    fun testMissingBindingFails() = codegen(
        """
        val TestComponent = Component("c") {
            factory<String> { get<Int>(); "" }
        }
    """
    ) {
        assertInternalError("missing")
    }

    @Test
    fun testDuplicatedScopeFails() = codegen(
        """
        val TestComponent = Component("c") {
            scope<TestScope>()
            scope<TestScope>()
        }
    """
    ) {
        assertInternalError("duplicate")
    }

    @Test
    fun testDuplicatedScopeInDifferentComponentsFails() = codegen(
        """
        val ComponentA = Component("a") {
            scope<TestScope>()
        }
        val ComponentB = Component("b") {
            parent("a", ComponentA)
            scope<TestScope>()
        }
    """
    ) {
        assertInternalError("duplicate")
    }

    @Test
    fun testMultipleScopes() = codegen(
        """
        val TestComponent = Component("c") {
            scope<TestScope>()
            scope<TestScope2>()
        }
    """
    ) {
        assertOk()
    }


    @Test
    fun testDuplicatedParentFails() = codegen(
        """
            val ParentComponent = Component("p") {} 
            
            val TestComponent = Component("c") {
                moduleA()
                moduleB()
            }
            
            @Module
            fun moduleA() {
                parent("p", ParentComponent)
            }
            
            @Module
            fun moduleB() {
                parent("p", ParentComponent)
            }
    """
    ) {
        assertInternalError("duplicate")
    }

    @Test
    fun testMultipleParents() = codegen(
        """
            val ParentComponentA = Component("a") {
                factory { "test" }
            } 
            val ParentComponentB = Component("b") {
                factory { 2 }
            } 

            val TestComponent = Component("c") {
                parent("a", ParentComponentA)
                parent("b", ParentComponentB)
            }
            
            fun invoke() = TestComponent.get<String>() to TestComponent.get<Int>()
    """
    ) {
        assertEquals("test" to 2, invokeSingleFile())
    }

    @Test
    fun testWithCaptures() = codegen(
        """
        fun TestComponent(capturedValue: String) = Component("c") {
            factory { capturedValue }
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testWithCaptureModule() = codegen(
        """
        @Module
        fun module(capturedValue: String) {
            factory { capturedValue }
        }
            
        fun TestComponent(capturedValue: String) = Component("c") { 
            module(capturedValue)
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testOrder() = codegen(
        """
            class Foo
            class Bar(foo: Foo)
            val TestComponent = Component("c") {
                factory { Foo() }
                factory { Bar(get()) }
                }
                fun invoke() = TestComponent

                """
    ) {
        expectNoErrorsWhileInvokingSingleFile()
    }

    @Test
    fun testReverseOrder() = codegen(
        """
            class Foo
            class Bar(foo: Foo)
            val TestComponent = Component("c") { 
                factory { Bar(get()) }
                factory { Foo() } 
                }
                
                fun invoke() = TestComponent
                """
    ) {
        expectNoErrorsWhileInvokingSingleFile()
    }

    @Test
    fun testFactory() = codegen(
        """
            val TestComponent = Component("c") { 
                factory { Foo() } 
                }
                
                fun invoke() = TestComponent.get<Foo>()
                """
    ) {
        assertNotSame(
            invokeSingleFile(),
            invokeSingleFile()
        )
    }

    @Test
    fun testSingle() = codegen(
        """
            val TestComponent = Component("c") { 
                single { Foo() } 
                }
                
                fun invoke() = TestComponent.get<Foo>()
                """
    ) {
        assertSame(
            invokeSingleFile(),
            invokeSingleFile()
        )
    }

    @Test
    fun testWithScope() = codegen(
        """
            val TestComponent = Component("c") {
                scope<TestScope>()
                }
                """
    ) {
        assertOk()
    }

    @Test
    fun testIntermediateModuleRequiredAddsAllModules() = codegen(
        """
            @Module
            fun rootModule() {
            }
            
            @Module
            fun intermediateModule(data: String) {
                factory { data }
                rootModule()
            }
            
            @Module
            fun dummy() {
            
            }
            
            @Module
            fun module() {
                dummy()
                intermediateModule("data")
            }
            
            val TestComponent = Component("c") {
                module()
                }
                """
    ) {
        assertOk()
    }

    @Test
    fun testAdvancedHierarchy() = codegen(
        """
            val GrandPa = Component("gp") {
                factory { 1L }
            }

            val Parent = Component("p") {
                factory { 2f }
                parent("gp", GrandPa)
                factory { 3 }
            }

            val Child = Component("c") {
                parent("p", Parent)
                factory { get<Int>().toString() }
            }
            
            fun invoke() = Child.get<Long>()
        """
    ) {
        assertEquals(1L, invokeSingleFile())
    }

    @Test
    fun testTransformGet() = codegen(
        """
        val TestComponent = Component("c") {
            factory { "test" }
        }
        
        fun invoke() = TestComponent.get<String>()
    """
    ) {
        assertEquals("test", invokeSingleFile())
    }

    @Test
    fun testTransformOwnerGet() = codegen(
        """
        val TestComponent = Component("c") {
            factory { "test" }
        }
        
        val owner = object : ComponentOwner {
            override val component = TestComponent
        }
        
        fun invoke() = owner.get<String>()
    """
    ) {
        assertEquals("test", invokeSingleFile())
    }

    @Test
    fun testComponentKeyDuplicate() = codegen(
        """
        val ComponentA = Component("key") { } 
        val ComponentB = Component("key") { }
        """
    ) {
        assertInternalError("exists")
    }

    @Test
    fun testSameModulesWithDifferentTypes() = codegen(
        """
            @Module
            inline fun <reified T> generic(value: T) {
                factory { value }
            }
            val Component = Component("key") {
                generic("test")
                generic(2)
            }
            fun invoke() = Component.get<String>() to Component.get<Int>()
        """
    ) {
        assertEquals("test" to 2, invokeSingleFile())
    }

    @Test
    fun testGenericDependsOnOther() = codegen(
        """
            @Module
            inline fun <reified A, reified B> module(a: A, b: B) {
                factory { a }
                factory { get<A>(); b }
            }
            val Component = Component("key") {
                module("test", 2)
            }
            fun invoke() = Component.get<String>() to Component.get<Int>()
        """
    ) {
        assertEquals("test" to 2, invokeSingleFile())
    }

    @Test
    fun testTypeDistinction() = codegen(
        """
        val TestComponent = Component("c") {
            factory { "" }
            factory { 0 }
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testQualifierDistinction() = codegen(
        """
        val TestComponent = Component("c") {
            factory { "a" }
            factory(TestQualifier1) { "b" }
            factory(TestQualifier2) { "c" }
        }
        
        fun invoke() = listOf(
            TestComponent.get<String>(), 
            TestComponent.get<String>(TestQualifier1), 
            TestComponent.get<String>(TestQualifier2)
            )
    """
    ) {
        assertEquals(
            listOf("a", "b", "c"),
            invokeSingleFile()
        )
    }

    @Test
    fun testQualifiedTransitiveDependency() = codegen(
        """
        val TestComponent = Component("c") {
            factory(TestQualifier1) { Foo() }
            factory { Bar(get(TestQualifier1)) }
        }
        
        fun invoke() = TestComponent.get<Bar>()
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testQualifierDistinctionInNestedComponents() = codegen(
        """
            val ComponentA = Component("a") {
                factory { "a" }
            }
            val ComponentB = Component("b") {
                parent("a", ComponentA)
                factory(TestQualifier1) { "b" }
            }
            val ComponentC = Component("c") { 
                parent("b", ComponentB)
                factory(TestQualifier2) { "c" }
            }
        
        fun invoke() = listOf(
            ComponentC.get<String>(), 
            ComponentC.get<String>(TestQualifier1), 
            ComponentC.get<String>(TestQualifier2)
            )
    """
    ) {
        assertEquals(
            listOf("a", "b", "c"),
            invokeSingleFile()
        )
    }


    //@Test
    fun testNullabilityDoesntMatter() = codegen(
        """
        val TestComponent = Component("c") {
            factory<String> { "" }
            factory<String?> { null }
        }
    """
    ) {
        assertInternalError("duplicate")
    }

    //@Test
    fun testMissingNullableBindingIsOk() = codegen(
        """
        val TestComponent = Component("c") {
            factory { get<String?>(); 0 }
        }
    """
    ) {
        assertOk()
    }

    /*@Test
    fun test() = codegenTest(
    """
            val TestComponent = Component("c") {
                factory { Foo() } 
                factory { Bar(get()) }
            }
            
            fun invoke() = TestComponent.get<Bar>()
    """
    ) {
        assertOk()
        invokeSingleFile() is Bar
    }*/

}
