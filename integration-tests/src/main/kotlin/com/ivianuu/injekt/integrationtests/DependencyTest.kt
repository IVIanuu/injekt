package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import org.junit.Test

class DependencyTest {

    @Test
    fun testDependencyAnnotatedDependency() = codegen(
        """
            @Component
            abstract class MyComponent(val fooContainer: FooContainer) {
                abstract val foo: Foo
            }
            
            @Dependency
            class FooContainer(val foo: Foo)
    """
    )

    @Test
    fun testDependencyAnnotatedProperty() = codegen(
        """
            @Component
            abstract class MyComponent(@Dependency val fooContainer: FooContainer) {
                abstract val foo: Foo
            }
            
            class FooContainer(val foo: Foo)
    """
    )

}
