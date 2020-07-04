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
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
import org.junit.Test

class FactoryTest {

    @Test
    fun testUnscoped() = codegen(
        """
        @Factory
        fun factory(): TestComponent1<Bar> {
            unscoped { Foo() }
            unscoped { Bar(get()) }
            return create()
        }
        
        val component = factory()
        fun invoke() = component.a
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
        @Factory
        fun factory(): TestComponent1<Foo> {
            scoped { Foo() }
            return create()
        }
        
        val component = factory()
        fun invoke() = component.a
    """
    ) {
        assertSame(
            invokeSingleFile(),
            invokeSingleFile()
        )
    }

    @Test
    fun testAnnotatedClass() = codegen(
        """
        @Unscoped class AnnotatedBar(foo: Foo)
        
        @Factory
        fun factory(): TestComponent1<AnnotatedBar> {
            unscoped<Foo>()
            return create()
        }
        
        val component = factory()
        fun invoke() = component.a
    """
    ) {
        assertNotSame(
            invokeSingleFile(),
            invokeSingleFile()
        )
    }

    @Test
    fun testBindingWithoutDefinition() = codegen(
        """
        @Factory
        fun factory(): TestComponent1<Bar> {
            unscoped<Foo>()
            unscoped<Bar>()
            return create()
        }
        
        val component = factory()
        fun invoke() = component.a
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testInclude() = codegen(
        """
        @Module
        fun module(foo: Foo) {
            unscoped { foo }
        }
        
        @Factory
        fun factory(foo: Foo): TestComponent1<Foo> {
            module(foo)
            return create()
        }
        
        fun invoke(foo: Foo) = factory(foo).a
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
            unscoped<Foo>()
            return create()
        }

        @Factory
        fun createChild(): TestComponent1<Bar> {
            dependency(createDep())
            unscoped { Bar(get()) }
            return create()
        }
        
        fun invoke() = createChild().a
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testAlias() = codegen(
        """
        @Factory
        fun factory(): TestComponent2<Any, Foo> {
            scoped { Foo() }
            alias<Foo, Any>()
            return create()
        }
        
        val component = factory()
        fun invoke() = component.a to component.b
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
        @Unscoped class Dep(val testComponent: TestComponent1<Dep>)
        
        @Factory
        fun factory(): TestComponent1<Dep> = create()
        
        fun invoke(): Pair<TestComponent1<Dep>, TestComponent1<Dep>> = factory().let {
            it to it.a.testComponent
        }
    """
    ) {
        val (component, dep) = invokeSingleFile<Pair<*, *>>()
        assertSame(component, dep)
    }

    @Test
    fun testGenericAnnotatedClass() = codegen(
        """
        @Unscoped class Dep<T>(val value: T)
        
        @Factory
        fun factory(): TestComponent2<Dep<String>, Dep<Int>> {
            unscoped { "hello world" }
            unscoped { 0 }
            return create()
        }
    """
    )

    @Test
    fun testModuleWithTypeArguments() = codegen(
        """
        @Module
        fun <T> generic(instance: T) {
            unscoped { instance }
        }

        @Factory
        fun factory(): TestComponent2<String, Int> { 
            generic("hello world")
            generic(42)
            return create()
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
            unscoped { get<T>() as S }
        }

        @Factory
        fun factory(): TestComponent1<Any> {
            unscoped<Foo>()
            unscoped<Bar>()
            diyAlias<Bar, Any>()
            return create()
        }
        
        fun invoke() = factory().a
         """
        ) {
            assertTrue(invokeSingleFile() is Bar)
        }

    @Test
    fun testComponentSuperTypeWithTypeParameters() =
        codegen(
            """
        interface BaseComponent<T> {
            val dep: Foo
        }
        
        interface ImplComponent : BaseComponent<Foo>
        
        @Factory
        fun createImplComponent(): ImplComponent {
            unscoped { Foo() }
            return create()
        }
    """
        )

    @Test
    fun testComponentWithGenericSuperType() = codegen(
        """
        interface TypedComponent<T> {
            val dep: T
        }

        @Factory
        fun createImplComponent(): TypedComponent<Foo> {
            unscoped { Foo() }
            return create()
        }
    """
    )

    @Test
    fun testFactoryWithDefaultParameters() = codegen(
        """
        @Factory
        fun factory(string: String = "default"): TestComponent1<String> {
            unscoped { string }
            return create()
        }
        
        fun invoke() = factory().a to factory("non_default").a
    """
    ) {
        val pair = invokeSingleFile<Pair<String, String>>()
        assertEquals("default", pair.first)
        assertEquals("non_default", pair.second)
    }

    @Test
    fun testComponentAsMemberFunction() = codegen(
        """
        class MyClass {
            val outerField = ""
            
            @Unscoped class Dep(myClass: MyClass, foo: Foo)
            @Factory
            fun createComponent(userId: String): TestComponent1<Dep> {
                unscoped<Foo>()
                myModule()
                return create()
            }
            
            @Module
            fun myModule() { 
                unscoped { outerField }
                myOtherModule()
            }
        }
        
        @Module 
        fun MyClass.myOtherModule() { 
            unscoped { this@myOtherModule } 
        }
        
        fun invoke() = MyClass().createComponent("")
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testComponentExtensionFunction() = codegen(
        """
        class MyClass {
            val outerField = ""
            @Unscoped class Dep(myClass: MyClass, foo: Foo)
        }

        @Factory 
        fun MyClass.createComponent(userId: String): TestComponent1<MyClass.Dep> { 
            unscoped<Foo>()
            myModule()
            return create()
        }
        
        @Module 
        fun MyClass.myModule() { 
            unscoped { outerField }
            myOtherModule() 
        }
        
        @Module 
        fun MyClass.myOtherModule() { 
            unscoped { this@myOtherModule } 
        }
        
        fun invoke() = MyClass().createComponent("")
    """
    ) {
        invokeSingleFile()
    }

    // todo @Test
    fun testLocalFunctionImplFactory() = codegen(
        """
        fun factory(): TestComponent1<Bar> {
            @Factory
            fun factory(): TestComponent1<Bar> {
                unscoped<Foo>()
                unscoped<Bar>()
                return create()
            }
            return factory()
        }
    """
    )

    @Test
    fun testChangedScopeNameForFactory() = codegen(
        """
        @Scoped<Any> class Dep

        @Factory(Any::class)
        fun factory(): TestComponent1<Foo> {
            scoped { Foo() }
            return create()
        }
    """
    )

    @Test
    fun testChangedScopeNameForChildFactory() = codegen(
        """
        @Scoped<Any> class Dep

        @ChildFactory(Any::class)
        fun child(): TestComponent1<Dep> {
            return create()
        }
        
        @Factory
        fun parent(): TestComponent1<Foo> {
            unscoped<Foo>()
            childFactory(::child)
            return create()
        }
    """
    )

}
