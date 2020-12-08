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
            @Binding suspend fun bar(foo: Foo) = Bar(foo)

            fun invoke(foo: Foo): Bar { 
                return runBlocking { create<suspend (Foo) -> Bar>()(foo) }
            }
    """
    ) {
        invokeSingleFile(Foo())
    }

    @Test
    fun testAssistedComposableBindingFunction() = codegen(
        """
            @Binding
            @Composable
            fun bar(foo: Foo) = Bar(foo)

            fun invoke(foo: Foo) { 
                create<@Composable (Foo) -> Bar>()
            }
    """
    ) {
        invokeSingleFile(Foo())
    }

    @Test
    fun testAssistedBindingFunction() = codegen(
        """
            @Binding fun bar(foo: Foo) = Bar(foo)

            fun invoke(foo: Foo): Bar { 
                return create<(Foo) -> Bar>()(foo)
            }
    """
    ) {
        invokeSingleFile(Foo())
    }

    @Test
    fun testComplexAssistedBindingFunction() = codegen(
        """
            @Binding fun bar(foo: Foo, string: String, int: Int) = Bar(foo)
            @Binding val string = ""
            
            @Component interface BarComponent {
                val barFactory: (Foo, Int) -> Bar
            }
            fun invoke(foo: Foo): Bar { 
                return create<BarComponent>().barFactory(foo, 0)
            }
    """
    ) {
        invokeSingleFile(Foo())
    }

    @Test
    fun testScopedAssistedBinding() = codegen(
        """
            @Scoped(TestScope1::class)
            @Binding fun bar(foo: Foo) = Bar(foo)

            private val createBar = create<@Scoped(TestScope1::class) (Foo) -> Bar>()

            fun invoke(): Pair<Bar, Bar> { 
                return createBar(Foo()) to createBar(Foo())
            }
    """
    ) {
        val (a, b) = invokeSingleFile<Pair<Bar, Bar>>()
        assertSame(a, b)
    }

    // todo @Test
    fun testScopedAssistedBindingInChild() = codegen(
        """
            @Component interface ParentComponent {
                val childFactory: () -> MyChildComponent
                @Binding(BarComponent::class)
                fun bar(foo: Foo) = Bar(foo)
            }
            
            @Component interface MyChildComponent {
                val barFactory: (Foo) -> Bar
            }
            
            private val parentComponent = create<BarComponent>()
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
            @Binding class AnnotatedBar(foo: Foo)
            
            @Component interface MyComponent {
                val annotatedBar: (Foo) -> AnnotatedBar
            }

            fun invoke(foo: Foo): AnnotatedBar = create<MyComponent>().annotatedBar(foo)
    """
    ) {
        invokeSingleFile(Foo())
    }

    @Test
    fun testRecursiveAssistedRequest() = codegen(
        """
            @Binding class MyBinding(
                val id: String,
                val myBindingFactory: (String) -> MyBinding
            )
            
            @Component interface MyComponent {
                val myBindingFactory: (String) -> MyBinding
            }

            fun invoke() {
                val component = create<MyComponent>()
                val myBindingA = component.myBindingFactory("a")
                val myBindingB = myBindingA.myBindingFactory("b")
            }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testNestedRecursiveAssistedRequest() = codegen(
        """
            @Binding class MyBindingA(
                val id: String,
                val myBindingFactoryB: (String) -> MyBindingB
            )

            @Binding class MyBindingB(
                val id: String,
                val myBindingFactoryA: (String) -> MyBindingA
            )
            
            @Component interface MyComponent {
                val myBindingAFactory: (String) -> MyBindingA
            }

            fun invoke() {
                val component = create<MyComponent>()
                val myBindingA = component.myBindingAFactory("a")
                val myBindingB = myBindingA.myBindingFactoryB("b")
                val myBindingA2 = myBindingB.myBindingFactoryA("a2")
            }
    """
    ) {
        invokeSingleFile()
    }

    // todo @Test
    fun testBindingRequestsAssistedFactoryOfItself() = codegen(
        """
            @Binding class MyBinding(
                val myBindingFactory: (MyBinding?) -> MyBinding,
                val parent: MyBinding?
            )
            
            @Component interface MyComponent {
                val myBindingFactory: (MyBinding?) -> MyBinding
            }

            fun invoke() {
                val component = create<MyComponent>()
                val myBindingA = component.myBindingFactory(null)
                val myBindingB = myBindingA.myBindingFactory(myBindingA)
            }
    """
    ) {
        invokeSingleFile()
    }

}
