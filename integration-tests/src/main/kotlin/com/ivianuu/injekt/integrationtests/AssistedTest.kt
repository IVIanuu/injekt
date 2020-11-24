package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Bar
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertSame
import org.junit.Test

class AssistedTest {

    @Test
    fun testAssistedSuspendBindingFunction() = codegen(
        """
            @Component
            abstract class BarComponent {
                abstract val barFactory: suspend (Foo) -> Bar
                
                @Binding
                protected suspend fun bar(foo: Foo) = Bar(foo)
            }

            fun invoke(foo: Foo): Bar { 
                return runBlocking { component<BarComponent>().barFactory(foo) }
            }
    """
    ) {
        invokeSingleFile(Foo())
    }

    @Test
    fun testAssistedComposableBindingFunction() = codegen(
        """
            @Component
            abstract class BarComponent {
                abstract val barFactory: @Composable (Foo) -> Bar
                
                @Binding
                @Composable
                protected fun bar(foo: Foo) = Bar(foo)
            }

            fun invoke(foo: Foo) { 
                component<BarComponent>().barFactory
            }
    """
    ) {
        invokeSingleFile(Foo())
    }

    @Test
    fun testAssistedBindingFunction() = codegen(
        """
            @Component
            abstract class BarComponent {
                abstract val barFactory: (Foo) -> Bar
                
                @Binding
                protected fun bar(foo: Foo) = Bar(foo)
            }

            fun invoke(foo: Foo): Bar { 
                return component<BarComponent>().barFactory(foo)
            }
    """
    ) {
        invokeSingleFile(Foo())
    }

    @Test
    fun testComplexAssistedBindingFunction() = codegen(
        """
            @Component
            abstract class BarComponent {
                abstract val barFactory: (Foo, Int) -> Bar
                
                @Binding
                protected fun bar(foo: Foo, string: String, int: Int) = Bar(foo)
                
                @Binding
                protected val string = ""
            }
            fun invoke(foo: Foo): Bar { 
                return component<BarComponent>().barFactory(foo, 0)
            }
    """
    ) {
        invokeSingleFile(Foo())
    }

    @Test
    fun testScopedAssistedBinding() = codegen(
        """
            @Component
            abstract class BarComponent {
                abstract val barFactory: (Foo) -> Bar
                
                @Binding(BarComponent::class)
                protected fun bar(foo: Foo) = Bar(foo)
            }
            
            private val component = component<BarComponent>()

            fun invoke(): Pair<Bar, Bar> { 
                return component.barFactory(Foo()) to component.barFactory(Foo())
            }
    """
    ) {
        val (a, b) = invokeSingleFile<Pair<Bar, Bar>>()
        assertSame(a, b)
    }

    // todo @Test
    fun testScopedAssistedBindingInChild() = codegen(
        """
            @Component
            abstract class ParentComponent {
                abstract val childFactory: () -> MyChildComponent
                @Binding(BarComponent::class)
                protected fun bar(foo: Foo) = Bar(foo)
            }
            
            @ChildComponent
            abstract class MyChildComponent {
                abstract val barFactory: (Foo) -> Bar
            }
            
            private val parentComponent = component<BarComponent>()
            private val childComponent = parentComponent.childFactory()

            fun invoke(): Pair<Bar, Bar> { 
                return childComponent.barFactory(Foo()) to childComponent.barFactory(Foo())
            }
    """
    ) {
        val (a, b) = invokeSingleFile<Pair<Bar, Bar>>()
        assertSame(a, b)
    }

    @Test
    fun testAssistedBindingClass() = codegen(
        """
            @Binding
            class AnnotatedBar(foo: Foo)
            
            @Component
            abstract class MyComponent {
                abstract val annotatedBar: (Foo) -> AnnotatedBar
            }

            fun invoke(foo: Foo): AnnotatedBar = component<MyComponent>().annotatedBar(foo)
    """
    ) {
        invokeSingleFile(Foo())
    }

    @Test
    fun testRecursiveAssistedRequest() = codegen(
        """
            @Binding
            class MyBinding(
                val id: String,
                val myBindingFactory: (String) -> MyBinding
            )
            
            @Component
            abstract class MyComponent {
                abstract val myBindingFactory: (String) -> MyBinding
            }
    """
    )

    @Test
    fun testNestedRecursiveAssistedRequest() = codegen(
        """
            @Binding
            class MyBindingA(
                val id: String,
                val myBindingFactoryB: (String) -> MyBindingB
            )

            @Binding
            class MyBindingB(
                val id: String,
                val myBindingFactoryA: (String) -> MyBindingA
            )
            
            @Component
            abstract class MyComponent {
                abstract val myBindingAFactory: (String) -> MyBindingA
            }
    """
    )

    @Test
    fun testBindingRequestsAssistedFactoryOfItself() = codegen(
        """
            @Binding
            class MyBinding(
                val myBindingFactory: (MyBinding?) -> MyBinding,
                val parent: MyBinding?
            )
            
            @Component
            abstract class MyComponent {
                abstract val myBindingFactory: (MyBinding?) -> MyBinding
            }
    """
    )

}
