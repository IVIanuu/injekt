package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import org.junit.Test

class MergeTest {

    @Test
    fun testMergeComponent() = codegen(
        """
            @MergeComponent
            abstract class MyComponent
            
            @MergeInto(MyComponent::class)
            @Module
            class MyModule {
                @Binding
                fun foo() = Foo()
            }
            
            @MergeInto(MyComponent::class)
            interface FooComponent {
                val foo: Foo
            }
            
            @GenerateMergeComponents
            fun invoke() {
                val component = MyComponentImpl()
                val fooComponent = component.mergeComponent<FooComponent>()
                fooComponent.foo
            }
        """
    )

}
