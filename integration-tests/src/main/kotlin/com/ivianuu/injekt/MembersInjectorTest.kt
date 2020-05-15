package com.ivianuu.injekt

import org.junit.Test

class MembersInjectorTest {

    @Test
    fun testMembersInjector() = codegen(
        """
        abstract class SuperClass { 
            val foo: Foo by inject()
        }
        class MyClass : SuperClass() { 
            val bar: Bar by inject()
        }
        
        interface TestComponent { 
            val injectMyClass: @MembersInjector (MyClass) -> Unit
            val bar: Bar
        }
        
        @Factory
        fun create(): TestComponent {
            transient { Foo() }
            scoped { Bar(get()) }
            return createImpl()
        }
        
        fun invoke() { 
            val testComponent = create()
            val myClass = MyClass()
            testComponent.injectMyClass(myClass)
            check(myClass.bar === testComponent.bar)
        }
    """
    ) {
        invokeSingleFile()
    }

}