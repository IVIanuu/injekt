package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.Component
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class ComponentTest {

    @Test
    fun testSimple() = codegenTest(
        """
        val MyComponent = Component("c") {

        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testWithDeps() = codegenTest(
        """
        val MyComponent = Component("c") {
            factory { "" }
            factory { 0 }
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testWithModule() = codegenTest(
        """
        @Module
        fun module() {
            factory { "" }
            factory { 0 }
        }
        
        val MyComponent = Component("c") {
            module()
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testWithNestedModule() = codegenTest(
        """
        @Module
        fun a() {
            factory { "" }
            b()
        }
        
        @Module
        fun b() { 
            factory { 0 }
        }
        
        val MyComponent = Component("c") {
            a()
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testDuplicatedBindingFails() = codegenTest(
        """
        val MyComponent = Component("c") {
            factory { "a" }
            factory { "b" }
        }
    """
    ) {
        assertInternalError("duplicate")
    }

    @Test
    fun testMissingBindingFails() = codegenTest(
        """
        val MyComponent = Component("c") {
            factory<String> { get<Int>(); "" }
        }
    """
    ) {
        assertInternalError("missing")
    }

    @Test
    fun testDuplicatedScopeFails() = codegenTest(
        """
        val MyComponent = Component("c") {
            scope<TestScope>()
            scope<TestScope>()
        }
    """
    ) {
        assertInternalError("duplicate")
    }

    @Test
    fun testMultipleScopes() = codegenTest(
        """
        val MyComponent = Component("c") {
            scope<TestScope>()
            scope<TestScope2>()
        }
    """
    ) {
        assertOk()
    }


    @Test
    fun testDuplicatedParentFails() = codegenTest(
        """
            val ParentComponent = Component("p") {} 
            
            val MyComponent = Component("c") {
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
    fun testMultipleParents() = codegenTest(
        """
            val ParentComponentA = Component("a") {} 
            val ParentComponentB = Component("b") {} 

            val MyComponent = Component("c") {
                parent("a", ParentComponentA)
                parent("b", ParentComponentB)
            }
    """
    ) {
        assertOk()
    }

    @Test
    fun testWithCaptures() = codegenTest(
        """
        fun MyComponent(capturedValue: String) = Component("c") {
            factory { capturedValue }
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testWithCaptureModule() = codegenTest(
        """
        @Module
        fun module(capturedValue: String) {
            factory { capturedValue }
        }
            
        fun MyComponent(capturedValue: String) = Component("c") { 
            module(capturedValue)
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testOrder() = codegenTest(
        """
            class Foo
            class Bar(foo: Foo)
            val MyComponent = Component("c") {
                factory { Foo() }
                factory { Bar(get()) }
                }
                fun invoke() = MyComponent

                """
    ) {
        expectNoErrorsWhileInvokingSingleFile()
    }

    @Test
    fun testReverseOrder() = codegenTest(
        """
            class Foo
            class Bar(foo: Foo)
            val MyComponent = Component("c") { 
                factory { Bar(get()) }
                factory { Foo() } 
                }
                
                fun invoke() = MyComponent
                """
    ) {
        expectNoErrorsWhileInvokingSingleFile()
    }

    @Test
    fun testFactory() = codegenTest(
        """
            val MyComponent = Component("c") { 
                factory { Foo() } 
                }
                
                fun invoke() = MyComponent
                """
    ) {
        val component = invokeSingleFile() as Component
        assertNotSame(
            component.get<Foo>("com.ivianuu.injekt.compiler.Foo".hashCode()),
            component.get<Foo>("com.ivianuu.injekt.compiler.Foo".hashCode())
        )
    }

    @Test
    fun testSingle() = codegenTest(
        """
            val MyComponent = Component("c") { 
                single { Foo() } 
                }
                
                fun invoke() = MyComponent
                """
    ) {
        val component = invokeSingleFile() as Component
        assertSame(
            component.get<Foo>("com.ivianuu.injekt.compiler.Foo".hashCode()),
            component.get<Foo>("com.ivianuu.injekt.compiler.Foo".hashCode())
        )
    }

    @Test
    fun testWithScope() = codegenTest(
        """
            val MyComponent = Component("c") {
                scope<TestScope>()
                }
                """
    ) {
        assertOk()
    }

    @Test
    fun testIntermediateModuleRequiredAddsAllModules() = codegenTest(
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
            
            val MyComponent = Component("c") {
                module()
                }
                """
    ) {
        assertOk()
    }

    @Test
    fun testAdvancedHierarchy() = codegenTest(
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
                println()
            }
            
            fun invoke() = Child
        """
    ) {
        invokeSingleFile<Component>().get("kotlin.Long".hashCode())
    }

    @Test
    fun testTransformGet() = codegenTest(
        """
        val MyComponent = Component("c") {
            factory { "" }
        }
        
        fun invoke() = MyComponent.get<String>()
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testTransformOwnerGet() = codegenTest(
        """
        val MyComponent = Component("c") {
            factory { "" }
        }
        
        val owner = object : ComponentOwner {
            override val component = MyComponent
        }
        
        fun invoke() = owner.get<String>()
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testComponentKeyDuplicate() = codegenTest(
        """
        val ComponentA = Component("key") { } 
        val ComponentB = Component("key") { }
        """
    ) {
        assertInternalError("exists")
    }

    @Test
    fun testSameModulesWithDifferentTypes() = codegenTest(
        """
            @Module
            inline fun <reified T> generic(value: T) {
                factory { value }
            }
            val Component = Component("key") {
                generic("")
                generic(0)
            }
        """
    ) {
    }

    /*@Test
    fun test() = codegenTest(
    """
            val MyComponent = Component("c") { 
                factory { Foo() } 
                factory { Bar(get()) }
            }
            
            fun invoke() = MyComponent.get<Bar>()
    """
    ) {
        assertOk()
        invokeSingleFile() is Bar
    }*/

}
