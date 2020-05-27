/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt

import com.ivianuu.injekt.test.Bar
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
import org.junit.Test

class ImplFactoryTest {

    @Test
    fun testTransient() = codegen(
        """
        interface TestComponent {
            val bar: Bar
        }
        
        @Factory
        fun createComponent(): TestComponent {
            transient { Foo() }
            transient { foo: Foo -> Bar(foo) }
            return create()
        }
        
        val component = createComponent()
        fun invoke() = component.bar
    """
    ) {
        assertNotSame(
            invokeSingleFile(),
            invokeSingleFile()
        )
    }

    @Test
    fun testTransientWithAnnotatedClass() = codegen(
        """
        @Transient class AnnotatedBar(foo: Foo)
        interface TestComponent {
            val bar: AnnotatedBar
        }
        
        @Factory
        fun createComponent(): TestComponent {
            transient<Foo>()
            return create()
        }
        
        val component = createComponent()
        fun invoke() = component.bar
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
        fun createComponent(): TestComponent {
            transient<Foo>()
            transient<Bar>()
            return create()
        }
        
        val component = createComponent()
        fun invoke() = component.bar
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
        fun createComponent(): TestComponent {
            scoped { Foo() }
            return create()
        }
        
        val component = createComponent()
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
        @InstanceFactory
        fun invoke(foo: Foo): Foo {
            instance(foo)
            return create()
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
        
        @InstanceFactory
        fun invoke(foo: Foo): Foo {
            module(foo)
            return create()
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
            return create()
        }
        
        interface TestComponent {
            val bar: Bar
        }

        @Factory
        fun createChild(): TestComponent {
            dependency(createDep())
            transient { foo: Foo -> Bar(foo) }
            return create()
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
        fun createComponent(): TestComponent {
            scoped { Foo() }
            alias<Foo, Any>()
            return create()
        }
        
        val component = createComponent()
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
        fun invoke(): TestComponent = create()
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
        fun createComponent(): TestComponent = create()
        
        fun invoke(): Pair<TestComponent, TestComponent> = createComponent().let {
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
        fun createComponent(): TestComponent {
            instance("hello world")
            instance(0)
            return create()
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
        fun createComponent(): TestComponent { 
            generic("hello world")
            generic(42)
            return create()
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testProviderDefinitionWhichUsesTypeParameters() =
        codegen(
            """
        @Module
        fun <T : S, S> diyAlias() {
            transient { get<T>() as S }
        }

        @InstanceFactory
        fun invoke(): Any {
            transient<Foo>()
            transient<Bar>()
            diyAlias<Bar, Any>()
            return create()
        }
         """
        ) {
            assertTrue(invokeSingleFile() is Bar)
        }

    @Test
    fun testComponentSuperTypeWithTypeParameters() =
        codegen(
            """
        interface BaseComponent<T> {
            val inject: @MembersInjector (T) -> Unit
        }
        
        class Injectable { 
            private val foo: Foo by inject()
        }
        
        interface ImplComponent : BaseComponent<Injectable>
        
        @Factory
        fun createImplComponent(): ImplComponent {
            transient { Foo() }
            return create()
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
            private val foo: Foo by inject()
        }

        @Factory
        fun createImplComponent(): TypedComponent<Injectable> {
            transient { Foo() }
            return create()
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
                return create()
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
            return create()
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
        fun createComponent(): TestComponent {
            @Factory
            fun factory(): TestComponent {
                transient<Foo>()
                transient<Bar>()
                return create()
            }
            return factory()
        }
    """
    )

    @Test
    fun testInlineFactory() = codegen(
        """
        interface TestComponent<T> {
            val dep: T
        }
        
        @Factory
        inline fun <T> createComponent(): TestComponent<T> {
            transient<Foo>()
            transient<Bar>()
            return create()
        }
        
        fun invoke() {
            createComponent<Foo>()
            createComponent<Bar>()
        }
    """
    )

    @Test
    fun testMultiCompilationInlineFactory() = multiCodegen(
        listOf(
            source(
                """
                    interface TestComponent<T> { 
                        val dep: T 
                    }
                    
                    @Factory 
                    inline fun <T> createComponent(): TestComponent<T> { 
                        transient<Foo>()
                        transient<Bar>()
                        return create()
                    }
                    """
            )
        ),
        listOf(
            source(
                """
                    fun invoke() { 
                        createComponent<Foo>()
                        createComponent<Bar>()
                    }
                """
            )
        )
    )

    @Test
    fun testMultiCompilationInlineFactoryWithSameName() = multiCodegen(
        listOf(
            source(
                """
                    interface TestComponent<T> { 
                        val dep: T 
                    }
                    
                    @Factory 
                    inline fun <T> createComponent(): TestComponent<T> { 
                        transient<Foo>()
                        transient<Bar>()
                        return create()
                    }
                    
                    @Factory 
                    inline fun <T> createComponent(block: @Module () -> Unit): TestComponent<T> {
                        block()
                        return create()
                    }
                    """
            )
        ),
        listOf(
            source(
                """
                    fun invoke() { 
                        createComponent<Foo>()
                        createComponent<Bar> {
                            transient<Foo>()
                            transient<Bar>()
                        }
                    }
                """
            )
        )
    )


    @Test
    fun testImplFactoryLambda() = codegen(
        """
        interface TestComponent { 
            val bar: Bar 
        }
        fun createComponent(): TestComponent {
            val factory = @Factory {
                transient<Foo>()
                transient<Bar>()
                create<TestComponent>()
            }
            return factory()
        }
    """
    )

    @Test
    fun testLocalChildFactoryInLocalParentFactory() =
        codegen(
            """
        interface ChildComponent { 
            val bar: Bar 
        }
        interface ParentComponent { 
            val childFactory: @ChildFactory () -> ChildComponent 
        }
        
        fun createComponent(): ChildComponent {
            @Factory
            fun parent(): ParentComponent {
                transient<Foo>()
                
                @ChildFactory
                fun child(): ChildComponent {
                    transient<Bar>()
                    return create()
                }
                
                childFactory(::child)
                
                return create()
            }
            return parent().childFactory()
        }
    """
        )

    @Test
    fun testFactoryWithDefaultParameters() = codegen(
        """
        interface TestComponent {
            val string: String
        }
        
        @Factory
        fun createComponent(string: String = "default"): TestComponent {
            instance(string)
            return create()
        }
        
        fun invoke() = createComponent().string to createComponent("non_default").string
    """
    ) {
        val pair = invokeSingleFile<Pair<String, String>>()
        assertEquals("default", pair.first)
        assertEquals("non_default", pair.second)
    }
}
