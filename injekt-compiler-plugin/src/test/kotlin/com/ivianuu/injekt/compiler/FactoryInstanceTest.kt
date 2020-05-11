package com.ivianuu.injekt.compiler

import org.junit.Test

class FactoryInstanceTest {

    @Test
    fun testCreateInstance() = codegen(
        """
        @Factory
        fun createBar(): Bar { 
            transient<Foo>()
            transient<Bar>()
            return createInstance()
        }
        
        fun invoke() = createBar()
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testCreateInstanceAdvanced() = codegen(
        """
        @TestScope 
        class MyClass(foo: Foo, bar: Bar)
        @Factory
        fun createBar(): MyClass {
            scope<TestScope>()
            scoped<Foo>()
            transient<Bar>()
            return createInstance()
        }
        
        fun invoke() = createBar()
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testDependencyInInstance() = codegen(
        """
        interface FooOwner {
            val foo: Foo
        }
        
        @Factory
        fun createFoo(fooOwner: FooOwner): Foo {
            dependency(fooOwner)
            return createInstance()
        }
    """
    )

    @Test
    fun testLocalFunctionInstanceFactory() = codegen(
        """
        fun create(): Bar {
            @Factory
            fun factory(): Bar {
                transient<Foo>()
                transient<Bar>()
                return createInstance()
            }
            return factory()
        }
    """
    )

    @Test
    fun testInstanceFactoryLambda() = codegen(
        """
        fun create(): Bar {
            val factory = @Factory {
                transient<Foo>()
                transient<Bar>()
                createInstance<Bar>()
            }
            return factory()
        }
    """
    )

    @Test
    fun testFactoryWithTypeParameters() = codegen(
        """
        @Factory
        inline fun <T> create(): T {
            transient<Foo>()
            transient<Bar>()
            return createInstance()
        }
        
        fun invoke() {
            create<Foo>()
            create<Bar>()
        }
    """
    )

    @Test
    fun testComponentAsMemberFunction() = codegen(
        """
        class MyClass {
            val outerField = ""
            
            @Transient class Dep(myClass: MyClass, foo: Foo)
            @Factory
            fun createComponent(userId: String): Dep {
                transient<Foo>()
                myModule()
                return createInstance()
            }
            
            @Module
            fun myModule() { 
                instance(outerField)
                myOtherModule()
            }
        }
        
        @Module 
        fun MyClass.myOtherModule() { 
            transient { this@myOtherModule } 
        }
        
        fun invoke() = MyClass().createComponent("")
    """
    ) {
        invokeSingleFile()
    }

}