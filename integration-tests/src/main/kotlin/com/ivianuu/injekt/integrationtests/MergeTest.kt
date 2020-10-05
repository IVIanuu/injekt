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
            class ProvideFooModule {
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

    @Test
    fun testMergeChildComponent() = codegen(
        """
            @Component
            abstract class MyParentComponent {
                abstract val myChildComponentFactory: () -> MyChildComponent
            }
            
            @MergeChildComponent
            abstract class MyChildComponent
            
            @MergeInto(MyChildComponent::class)
            @Module
            class ProvideFooModule {
                @Binding
                fun foo() = Foo()
            }
            
            @MergeInto(MyChildComponent::class)
            interface FooComponent {
                val foo: Foo
            }
            
            @GenerateMergeComponents
            fun invoke() {
                val parentComponent = MyParentComponentImpl()
                val childComponent = parentComponent.myChildComponentFactory()
                val fooComponent = childComponent.mergeComponent<FooComponent>()
                fooComponent.foo
            }
        """
    )

}
