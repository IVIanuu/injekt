package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import org.junit.Test

class ModuleTest {

    @Test
    fun testExplicitModule() = codegen(
        """
            class MyModule {
                @Binding val foo: Foo get() = Foo()
            }

            fun invoke() = create<Foo>(MyModule())
        """
    )

    @Test
    fun testImplicitModule() = codegen(
        """
            @Module class MyModule {
                @Binding val foo: Foo get() = Foo()
            }

            fun invoke() = create<Foo>()
        """
    )

    @Test
    fun testCanDeclareExplicitModuleMultipleTimes() = codegen(
        """
            class MyModule {
                @Binding val foo: Foo get() = Foo()
            }

            class Module1 {
                @Module val myModule = MyModule()
            }

            class Module2 {
                @Module val myModule = MyModule()
            }

            fun invoke() = create<Foo>(Module1(), Module2())
        """
    )

    @Test
    fun testImplicitModuleCanRequestDependencies() = codegen(
        """
            @Module
            class MyModule(private val foo: Foo) {
                @Binding val bar: Bar get() = Bar(foo)
            }

            @Binding fun foo() = Foo()

            @Component interface MyComponent {
                val bar: Bar
            }
        """
    )

}
