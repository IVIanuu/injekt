package com.ivianuu.injekt.compiler

import org.junit.Test

class MembersInjectorTest {

    @Test
    fun testMembersInjector() = codegen(
        """
        abstract class SuperClass {
            @Inject lateinit var foo: Foo
        }
        class MyClass : SuperClass() {
            @Inject lateinit var bar: Bar
        }
        
        interface TestComponent { 
            val injectMyClass: @MembersInjector (MyClass) -> Unit
            val foo: Foo
            val bar: Bar
        }
        
        @Factory
        fun create(): TestComponent = createImplementation {
            scoped { Foo() }
            scoped { Bar(get()) }
        }
        
        fun invoke() { 
            val testComponent = create()
            val myClass = MyClass()
            testComponent.injectMyClass(myClass)
            check(myClass.bar === testComponent.bar)
            check(myClass.foo === testComponent.foo)
        }
    """
    ) {
        assertOk()
    }

}