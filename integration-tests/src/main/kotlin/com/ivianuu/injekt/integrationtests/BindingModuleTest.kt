package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import org.junit.Test

class BindingModuleTest {

    @Test
    fun testBindingModuleWithClass() = codegen(
        """
            @BindingModule(MyComponent::class)
            annotation class AnyBinding {
                @Module
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
    fun testBindingModuleWithAssistedClass() = codegen(
        """
            @BindingModule(MyComponent::class)
            annotation class AnyBinding {
                @Module
                class Impl<T : Any> {
                    @Binding
                    val T.any: Any get() = this
                }
            }
            
            @AnyBinding
            class AnnotatedBar(val foo: @Assisted Foo)
            
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
    fun testBindingModuleWithTopLevelFunction() = codegen(
        """
            @BindingModule(MyComponent::class)
            annotation class AnyBinding {
                @Module
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
    fun testBindingModuleWithAssistedTopLevelFunction() = codegen(
        """
            @BindingModule(MyComponent::class)
            annotation class AnyBinding {
                class Impl<T : Any> {
                    @Binding
                    val T.any: Any get() = this
                }
            }

            @AnyBinding
            fun myService(foo: @Assisted Foo) {
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

    @Test
    fun testBindingModuleWithFunctionInObject() = codegen(
        """
            @BindingModule(MyComponent::class)
            annotation class AnyBinding {
                @Module
                class Impl<T : Any> {
                    @Binding
                    val T.any: Any get() = this
                }
            }
            
            object MyObject {
                @AnyBinding
                fun myService(foo: Foo) {
                }
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
    fun testBindingModuleWithAssistedFunctionInObject() = codegen(
        """
            @BindingModule(MyComponent::class)
            annotation class AnyBinding {
                class Impl<T : Any> {
                    @Binding
                    val T.any: Any get() = this
                }
            }

            object MyObject {
                @AnyBinding
                fun myService(foo: @Assisted Foo) {
                }
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

    @Test
    fun testBindingModuleWithTopLevelFunBinding() = codegen(
        """
            @BindingModule(MyComponent::class)
            annotation class AnyBinding {
                @Module
                class Impl<T : () -> Unit> {
                    @Binding
                    val T.any: Any get() = this
                }
            }
            
            @AnyBinding
            @FunBinding
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
    fun testBindingModuleWithAssistedTopLevelFunBinding() = codegen(
        """
            @BindingModule(MyComponent::class)
            annotation class AnyBinding {
                class Impl<T : (Foo) -> Unit> {
                    @Binding
                    val T.any: Any get() = this
                }
            }

            @AnyBinding
            @FunBinding
            fun myService(foo: @Assisted Foo) {
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

    @Test
    fun testBindingModuleWithFunBindingInObject() = codegen(
        """
            @BindingModule(MyComponent::class)
            annotation class AnyBinding {
                @Module
                class Impl<T : () -> Unit> {
                    @Binding
                    val T.any: Any get() = this
                }
            }
            
            object MyObject {
                @AnyBinding
                @FunBinding
                fun myService(foo: Foo) {
                }
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
    fun testBindingModuleWithAssistedFunBindingInObject() = codegen(
        """
            @BindingModule(MyComponent::class)
            annotation class AnyBinding {
                class Impl<T : (Foo) -> Unit> {
                    @Binding
                    val T.any: Any get() = this
                }
            }

            object MyObject {
                @AnyBinding
                @FunBinding
                fun myService(foo: @Assisted Foo) {
                }
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
