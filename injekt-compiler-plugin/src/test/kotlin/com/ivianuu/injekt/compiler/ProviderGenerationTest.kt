package com.facebook.buck.jvm.java.javax.com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.codegen
import org.junit.Test

class ProviderGenerationTest {

    @Test
    fun testProvideOnClass() = codegen("""
        @Provide
        class SimpleClass(val foo: Foo, val bar: Bar)
    """)

    @Test
    fun testProvideOnClassConstructor() = codegen("""
        class SimpleClass @Provide constructor(val foo: Foo, val bar: Bar)
    """)

    @Test
    fun testStaticProvideInModule() = codegen("""
        @Module
        object MyModule {
            @Provide
            fun foo() = Foo()
        }
    """)

    @Test
    fun testStaticProvideWithDependenciesInModule() = codegen("""
        @Module
        object MyModule {
            @Provide
            fun bar(foo: Foo) = Bar(foo)
        }
    """)

    @Test
    fun testNonStaticProvideInModule() = codegen("""
        @Module
        class MyModule {
            @Provide
            fun foo() = Foo()
        }
    """)

    @Test
    fun testNonStaticProvideWithDependenciesInModule() = codegen("""
        @Module
        class MyModule {
            @Provide
            fun bar(foo: Foo) = Bar(foo)
        }
    """)

}
