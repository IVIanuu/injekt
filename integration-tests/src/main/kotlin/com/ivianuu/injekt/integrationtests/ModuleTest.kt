package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import org.junit.Test

class ModuleTest {

    @Test
    fun testExplicitModule() = codegen(
        """
            class MyModule {
                @Binding
                val foo: Foo get() = Foo()
            }

            @Component
            abstract class MyComponent {
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
                @Binding
                val foo: Foo get() = Foo()
            }

            @Component
            abstract class MyComponent {
                abstract val foo: Foo
            }
        """
    )

}
