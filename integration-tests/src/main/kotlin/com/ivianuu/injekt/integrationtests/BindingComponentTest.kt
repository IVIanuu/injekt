package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import org.junit.Test

class BindingComponentTest {

    @Test
    fun testBindingComponentWithClass() = codegen(
        """
            @BindingComponent(MyComponent::class)
            annotation class AnyBinding {
                class Impl<T : Any> {
                    @Binding
                    val T.any: Any get() = this
                }
            }
            
            @AnyBinding
            class AnnotatedBar(val foo: Foo)
            
            @MergeComponent
            abstract class MyComponent {
                abstract val any: Any
                
                @Binding protected fun foo() = Foo()
            }
            
            @GenerateMergeComponents
            fun invoke() {
            }
        """
    )

    @Test
    fun testBindingComponentWithAssistedClass() = codegen(
        """
            @BindingComponent(MyComponent::class)
            annotation class AnyBinding {
                class Impl<T : Any> {
                    @Binding
                    val T.any: Any get() = this
                }
            }
            
            @AnyBinding
            class AnnotatedBar(@Assisted val foo: Foo)
            
            @MergeComponent
            abstract class MyComponent {
                abstract val any: Any
            }
            
            @GenerateMergeComponents
            fun invoke() {
            }
        """
    )

    @Test
    fun testBindingComponentWithTopLevelFunction() = codegen(
        """
            @BindingComponent(MyComponent::class)
            annotation class AnyBinding {
                class Impl<T : Any> {
                    @Binding
                    val T.any: Any get() = this
                }
            }
            
            @AnyBinding
            fun myService(foo: Foo) {
            }

            @MergeComponent
            abstract class MyComponent {
                abstract val any: Any
                
                @Binding protected fun foo() = Foo()
            }
            
            @GenerateMergeComponents
            fun invoke() {
            }
        """
    )

    @Test
    fun testBindingComponentWithAssistedTopLevelFunction() = codegen(
        """
            @BindingComponent(MyComponent::class)
            annotation class AnyBinding {
                class Impl<T : Any> {
                    @Binding
                    val T.any: Any get() = this
                }
            }

            @AnyBinding
            fun myService(@Assisted foo: Foo) {
            }
            
            @MergeComponent
            abstract class MyComponent {
                abstract val any: Any
            }
            
            @GenerateMergeComponents
            fun invoke() {
            }
        """
    )

}
