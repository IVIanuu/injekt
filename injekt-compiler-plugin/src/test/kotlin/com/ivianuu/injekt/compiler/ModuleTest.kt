package com.ivianuu.injekt.compiler

import org.junit.Test

class ModuleTest {

    @Test
    fun testValueParameterCapturingModule() = codegen(
        """
        @Module
        fun capturingModule(capture: String) {
            transient { capture }
        }
        
        @Factory
        fun create(): String {
            capturingModule("hello world")
            return createInstance()
        }
    """
    )

    @Test
    fun testTypeParameterCapturingModule() = codegen(
        """
        @Module
        fun <T> capturingModule() {
            transient<@TestQualifier1 T> { get<T>() }
        }
        
        @Factory
        fun create(): @TestQualifier1 String {
            transient { "hello world" }
            capturingModule<String>()
            return createInstance()
        }
    """
    )

    @Test
    fun testLocalDeclarationCapturing() = codegen(
        """
        @Module
        fun capturingModule(greeting: String) {
            val local = greeting + " world"
            transient { local }
        }

        @Factory
        fun create(): String {
            capturingModule("hello")
            return createInstance()
        }
    """
    )

    @Test
    fun testMultipleModulesWithSameName() = codegen(
        """
        @Module
        fun module() {
        }
        
        @Module
        fun module(p0: String) {
        }
    """
    )

    @Test
    fun testIncludeLocalModule() = codegen(
        """
        @Module
        fun outer() {
            @Module
            fun <T> inner(instance: T) {
                instance(instance)
            }
            
            inner("hello world")
            inner(42)
        }
    """
    )

    @Test
    fun testClassOfModule() = codegen(
        """
        @Module
        inline fun <S : Any> classOfA() {
            val classOf = classOf<S>()
        }
        
        @Module
        inline fun <T : Any, V : Any> classOfB() {
            val classOf = classOf<T>()
            classOfA<V>()
        }
        
        @Module
        fun callingModule() {
            classOfB<String, Int>()
        }
    """
    )

    @Test
    fun testBindingWithTypeParameterInInlineModule() = codegen(
        """ 
        @Module
        inline fun <T> module() {
            transient<T>()
        }
    """
    )

}