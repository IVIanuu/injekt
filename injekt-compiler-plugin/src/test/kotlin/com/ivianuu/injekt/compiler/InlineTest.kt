package com.ivianuu.injekt.compiler

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test

class InlineTest {

    @Test
    fun testInlineModuleLambda() = codegen(
        """
        @Factory
        inline fun <T> buildInstance(block: @Module () -> Unit): T { 
            block()
            return createInstance() 
        }
        
        fun invoke() = buildInstance<Foo> { transient<Foo>() }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testNestedInlineModuleLambda() = codegen(
        """
        @Factory
        inline fun <T> buildInstance(block: @Module () -> Unit): T {
            nested(block)
            return createInstance() 
        }
        
        @Module
        inline fun nested(block: @Module () -> Unit) {
            block()
        }
        
        fun invoke() = buildInstance<Foo> { transient<Foo>() }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testInlineModuleLambdaWithArgs() = codegen(
        """
        @Module
        fun calling() {
            inlined { instance(it) }
        }
        
        @Module
        inline fun inlined(block: @Module (String) -> Unit) {
            block("hello world")
        }
        
        @Factory
        fun invoke(): String {
            calling()
            return createInstance()
        }
    """
    ) {
        assertEquals("hello world", invokeSingleFile())
    }

    @Test
    fun testNestedInlineModuleLambdaWithArgs() = codegen(
        """
        @Module
        fun calling() {
            inlined { instance(it) }
        }
        
        @Module
        inline fun inlined(block: @Module (String) -> Unit) {
            nestedInlined(block)
        }
        
        @Module
        inline fun nestedInlined(block: @Module (String) -> Unit) {
            block("hello world")
        }
        
        @Factory
        fun invoke(): String {
            calling()
            return createInstance()
        }
    """
    ) {
        assertEquals("hello world", invokeSingleFile())
    }

    @Test
    fun testFactoryWithModuleParam() = codegen(
        """
        @Factory
        inline fun factory(block: @Module () -> Unit): Foo {
            block()
            return createInstance()
        }
        
        fun invoke() = factory { 
            transient<Foo>() 
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testFactoryWithModuleParamInClass() = codegen(
        """
        class Lol {
            @Factory
            inline fun factory(block: @Module () -> Unit): Foo {
                block()
                return createInstance()
            }
        }
        
        fun invoke() = Lol().factory { 
            transient<Foo>() 
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testCapturingModuleLambda() = codegen(
        """
        interface TestComponent<T> {
            val dep: T
        }
        @Factory
        inline fun <T> createComponent(block: @Module () -> Unit): TestComponent<T> {
            block()
            return createImpl()
        }
        
        class MyClass {
            init {
                createComponent<MyClass> {
                    instance(this)
                }
            }
        }
    """
    )

    @Test
    fun testCapturingLocalModule() = codegen(
        """
        class MyClass {
            init {
                @Module
                fun localModule() {
                    instance(this)
                }
                @Factory
                fun create(): MyClass {
                    localModule()
                    return createInstance()
                }
            }
        }
    """
    )

    @Test
    fun testCapturingInnerClassLocalModule() = codegen(
        """
        interface TestComponent {
            val outer: OuterClass
            val inner: OuterClass.InnerClass
        }
        class OuterClass {
            inner class InnerClass {
                init { 
                    @Module 
                    fun localModule() { 
                        instance(this@OuterClass)
                        transient { this@InnerClass }
                    } 
                    @Factory 
                    fun create(): TestComponent { 
                        localModule()
                        return createImpl() 
                    } 
                }   
            }
        }
    """
    )

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

    @Test
    fun testCapturingMemberFunction() = codegen(
        """
        class MyClass {
            @Factory
            fun factory(): MyClass { 
                transient { this@MyClass }
                return createInstance()
            }
        }
    """
    )

    @Test
    fun testCapturingMemberAndExtensionFunction() = codegen(
        """
        class MyClass {
            @Factory
            fun String.factory(): Pair<MyClass, String> { 
                transient { this@MyClass to this@factory }
                return createInstance()
            }
        }
    """
    )

    @Test
    fun testCapturingMemberAndExtensionInLocalFunction() = codegen(
        """
        class MyClass {
            @Factory
            fun String.factory(): Pair<MyClass, String> { 
                @Module
                fun local() {
                    transient { this@MyClass to this@factory }
                }
                local()
                return createInstance()
            }
        }
    """
    )

}
