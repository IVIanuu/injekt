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

            @Component abstract class MyComponent {
                @Module protected val myModule = MyModule()
                abstract val foo: Foo
            }
        """
    )

    @Test
    fun testImplicitModule() = codegen(
        """
            @Module
            class MyModule {
                @Binding val foo: Foo get() = Foo()
            }

            @Component abstract class MyComponent {
                abstract val foo: Foo
            }
        """
    )

    @Test
    fun testCanDeclareExplicitModuleMultipleTimes() = codegen(
        """
            class MyModule {
                @Binding val foo: Foo get() = Foo()
            }

            @Component abstract class MyComponent {
                @Module protected val myModule = MyModule()
                @Module protected val myModule1 = MyModule()
                abstract val foo: Foo
            }
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

            @Component abstract class MyComponent {
                abstract val bar: Bar
            }
        """
    )

}
