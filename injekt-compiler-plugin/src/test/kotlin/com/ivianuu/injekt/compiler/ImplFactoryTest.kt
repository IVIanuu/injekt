package com.ivianuu.injekt.compiler

import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
import org.junit.Test

class ImplFactoryTest {

    @Test
    fun testTransient() = codegen(
        """
        interface TestComponent {
            val foo: Foo
        }
        
        @Factory
        fun create(): TestComponent {
            transient { Foo() }
            return createImpl()
        }
        
        fun invoke() = create().foo
    """
    ) {
        assertNotSame(
            invokeSingleFile(),
            invokeSingleFile()
        )
    }

    @Test
    fun testTransientWithoutDefinition() = codegen(
        """
        interface TestComponent {
            val bar: Bar
        }
        
        @Factory
        fun create(): TestComponent {
            transient<Foo>()
            transient<Bar>()
            return createImpl()
        }
        
        fun invoke() = create().bar
    """
    ) {
        assertNotSame(
            invokeSingleFile(),
            invokeSingleFile()
        )
    }

    @Test
    fun testScoped() = codegen(
        """
        interface TestComponent {
            val foo: Foo
        }
        
        @Factory
        fun create(): TestComponent {
            scoped { Foo() }
            return createImpl()
        }
        
        val component = create()
        fun invoke() = component.foo
    """
    ) {
        assertSame(
            invokeSingleFile(),
            invokeSingleFile()
        )
    }

    @Test
    fun testInstance() = codegen(
        """
        @Factory
        fun invoke(foo: Foo): Foo {
            instance(foo)
            return createInstance()
        }
         """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testInclude() = codegen(
        """
        @Module
        fun module(foo: Foo) {
            instance(foo)
        }
        
        @Factory
        fun invoke(foo: Foo): Foo {
            module(foo)
            return createInstance()
        }
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testDependency() = codegen(
        """
        interface DependencyComponent {
            val foo: Foo
        }
        
        @Factory
        fun createDep(): DependencyComponent {
            transient { Foo() }
            return createImpl()
        }
        
        interface TestComponent {
            val bar: Bar
        }

        @Factory
        fun createChild(): TestComponent {
            dependency(createDep())
            transient { Bar(get()) }
            return createImpl()
        }
        
        fun invoke() = createChild().bar
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testAlias() = codegen(
        """
        interface TestComponent {
            val any: Any
            val foo: Foo
        }
        
        @Factory
        fun create(): TestComponent {
            scoped { Foo() }
            alias<Foo, Any>()
            return createImpl()
        }
        
        val component = create()
        fun invoke() = component.foo to component.any
    """
    ) {
        val (foo, any) = (invokeSingleFile() as Pair<Foo, Any>)
        assertSame(foo, any)
    }

    @Test
    fun testEmpty() = codegen(
        """
        interface TestComponent {
        }
        
        @Factory
        fun create(): TestComponent = createImpl()
        
        fun invoke() = create()
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testFactoryImplementationBinding() = codegen(
        """
        interface TestComponent {
            val dep: Dep
        }
        
        @Transient class Dep(val testComponent: TestComponent)
        
        @Factory
        fun create(): TestComponent = createImpl()
        
        fun invoke(): Pair<TestComponent, TestComponent> = create().let {
            it to it.dep.testComponent
        }
    """
    ) {
        val (component, dep) = invokeSingleFile<Pair<*, *>>()
        assertSame(component, dep)
    }

    @Test
    fun testGenericAnnotatedClass() = codegen(
        """
        interface TestComponent {
            val stringDep: Dep<String> 
            val intDep: Dep<Int>
        }
        
        @Transient class Dep<T>(val value: T)
        
        @Factory
        fun create(): TestComponent {
            instance("hello world")
            instance(0)
            return createImpl()
        }
    """
    )

    @Test
    fun testModuleWithTypeArguments() = codegen(
        """
        interface TestComponent {
            val string: String
            val int: Int
        }
        
        @Module
        fun <T> generic(instance: T) {
            instance(instance)
        }

        @Factory
        fun create(): TestComponent { 
            generic("hello world")
            generic(42)
            return createImpl()
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testProviderDefinitionWhichUsesTypeParameters() = codegen(
        """
        @Module
        fun <T : S, S> diyAlias() {
            transient { get<T>() as S }
        }

        @Factory
        fun invoke(): Any {
            transient<Foo>()
            transient<Bar>()
            diyAlias<Bar, Any>()
            return createInstance()
        }
         """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testComponentSuperTypeWithTypeParameters() = codegen(
        """
        interface BaseComponent<T> {
            val inject: @MembersInjector (T) -> Unit
        }
        
        class Injectable {
            @Inject private lateinit var foo: Foo
        }
        
        interface ImplComponent : BaseComponent<Injectable>
        
        @Factory
        fun createImplComponent(): ImplComponent {
            transient { Foo() }
            return createImpl()
        }
    """
    )

    @Test
    fun testComponentWithGenericSuperType() = codegen(
        """
        interface TypedComponent<T> {
            val inject: @MembersInjector (T) -> Unit
        }
        
        class Injectable {
            @Inject private lateinit var foo: Foo
        }

        @Factory
        fun createImplComponent(): TypedComponent<Injectable> {
            transient { Foo() }
            return createImpl()
        }
    """
    )

    @Test
    fun testComponentAsMemberFunction() = codegen(
        """
        interface TestComponent {
            val dep: MyClass.Dep
        }

        class MyClass {
            val outerField = ""
            
            @Transient class Dep(myClass: MyClass, foo: Foo)
            @Factory
            fun createComponent(userId: String): TestComponent {
                transient<Foo>()
                myModule()
                return createImpl()
            }
            
            @Module
            fun myModule() { 
                instance(outerField)
                myOtherModule()
            }
        }
        
        @Module 
        fun MyClass.myOtherModule() { 
            transient { this@myOtherModule } 
        }
        
        fun invoke() = MyClass().createComponent("")
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testComponentExtensionFunction() = codegen(
        """
        interface TestComponent {
            val dep: MyClass.Dep
        }

        class MyClass {
            val outerField = ""
            @Transient class Dep(myClass: MyClass, foo: Foo)
        }

        @Factory 
        fun MyClass.createComponent(userId: String): TestComponent { 
            transient<Foo>()
            myModule()
            return createImpl()
        }
        
        @Module 
        fun MyClass.myModule() { 
            instance(outerField)
            myOtherModule() 
        }
        
        @Module 
        fun MyClass.myOtherModule() { 
            transient { this@myOtherModule } 
        }
        
        fun invoke() = MyClass().createComponent("")
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testLocalFunctionImplFactory() = codegen(
        """
        interface TestComponent { 
            val bar: Bar 
        }
        fun create(): TestComponent {
            @Factory
            fun factory(): TestComponent {
                transient<Foo>()
                transient<Bar>()
                return createImpl()
            }
            return factory()
        }
    """
    )

    @Test
    fun testFactoryWithTypeParameters() = codegen(
        """
        interface TestComponent<T> {
            val dep: T
        }
        
        @Factory
        inline fun <T> create(): TestComponent<T> {
            transient<Foo>()
            transient<Bar>()
            return createImpl()
        }
        
        fun invoke() {
            create<Foo>()
            create<Bar>()
        }
    """
    )

    @Test
    fun testImplFactoryLambda() = codegen(
        """
        interface TestComponent { 
            val bar: Bar 
        }
        fun create(): TestComponent {
            val factory = @Factory {
                transient<Foo>()
                transient<Bar>()
                createImpl<TestComponent>()
            }
            return factory()
        }
    """
    )

    @Test
    fun testLocalChildFactoryInLocalParentFactory() = codegen(
        """
        interface ChildComponent { 
            val bar: Bar 
        }
        interface ParentComponent { 
            val childFactory: @ChildFactory () -> ChildComponent 
        }
        
        fun create(): ChildComponent {
            @Factory
            fun parent(): ParentComponent {
                transient<Foo>()
                
                @ChildFactory
                fun child(): ChildComponent {
                    transient<Bar>()
                    return createImpl()
                }
                
                childFactory(::child)
                
                return createImpl()
            }
            return parent().childFactory()
        }
    """
    )
}
