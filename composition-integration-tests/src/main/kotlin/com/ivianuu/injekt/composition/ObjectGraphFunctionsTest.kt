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

package com.ivianuu.injekt.composition

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert.assertTrue
import org.junit.Test

class ObjectGraphFunctionsTest {

    @Test
    fun testInlineGetWithGenericComponentAndGenericInstance() =
        codegen(
            """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        fun <C : Any, T> getInstance(component: C): T {
            return component.get<T>()
        }
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return getInstance<TestCompositionComponent, Foo>(component)
        }
    """
        ) {
            assertTrue(invokeSingleFile() is Foo)
        }

    @Test
    fun testInlineGetWithGenericInstance() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        fun <T> getInstance(component: TestCompositionComponent): T {
            return component.get<T>()
        }
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return getInstance<Foo>(component)
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testInlineGetWithGenericComponent() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        fun <C : Any> getInstance(component: C): Foo {
            return component.get<Foo>()
        }
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return getInstance(component)
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testNestedInlineGetWithGenericComponentAndGenericInstance() =
        codegen(
            """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        fun <C : Any, T> getInstance(component: C): T {
            return component.get<T>()
        }
        
        fun <C : Any, T> nestedGetInstance(component: C): T {
            return getInstance<C, T>(component)
        }
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return nestedGetInstance<TestCompositionComponent, Foo>(component)
        }
    """
        ) {
            assertTrue(invokeSingleFile() is Foo)
        }

    @Test
    fun testNestedInlineGetWithGenericInstance() =
        codegen(
            """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        fun <C : Any, T> getInstance(component: C): T {
            return component.get<T>()
        }
        
        fun <T> nestedGetInstance(component: TestCompositionComponent): T {
            return getInstance<TestCompositionComponent, T>(component)
        }
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return nestedGetInstance<Foo>(component)
        }
    """
        ) {
            assertTrue(invokeSingleFile() is Foo)
        }

    @Test
    fun testNestedInlineGetWithGenericComponent() =
        codegen(
            """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        fun <C : Any, T> getInstance(component: C): T {
            return component.get<T>()
        }
        
        fun <C : Any> nestedGetInstance(component: C): Foo {
            return getInstance<C, Foo>(component)
        }
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return getInstance(component)
        }
    """
        ) {
            assertTrue(invokeSingleFile() is Foo)
        }

    @Test
    fun testMultiCompileObjectGraphGet() = multiCodegen(
        listOf(
            source(
                """
                @CompositionFactory 
                fun factory(): TestCompositionComponent { 
                    transient { Foo() }
                    return create() 
                }
                """
            )
        ),
        listOf(
            source(
                """
                fun <C : Any, T> getInstance(component: C): T { 
                    return component.get<T>() 
                }
            """
            )
        ),
        listOf(
            source(
                """
                fun invoke(): Foo { 
                    initializeCompositions() 
                    val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
                    return getInstance<TestCompositionComponent, Foo>(component)
                } 
            """
            )
        )
    )

    @Test
    fun testGetWithLambda() = codegen(
        """
        fun getComponent(): TestCompositionComponent = error("lol")
        fun <T> inject(): T {
            val component = getComponent()
            return memo { component.get<T>() }
        }
        
        fun <T> memo(init: () -> T): T = init()
        
        fun invoke() {
            inject<String>()
        }
    """
    )

    @Test
    fun testGetWithLocalFunction() = codegen(
        """
        fun getComponent(): TestCompositionComponent = error("lol")
        fun <T> inject(): T {
            val component = getComponent()
            
            fun <S> localGet(): S = component.get<S>()
            
            return localGet<T>()
        }
        
        fun invoke() {
            inject<String>()
        }
    """
    )

    @Test
    fun testGetWithInlineLambda() = codegen(
        """
        fun getComponent(): TestCompositionComponent = error("lol")
        fun <T> inject(): T {
            val component = getComponent()
            return memo { component.get<T>() }
        }
        
        inline fun <T> memo(init: () -> T): T = init()
        
        fun invoke() {
            inject<String>()
        }
    """
    )

    @Test
    fun testDeeplyNestedGet() = codegen(
        """
        fun getComponent(): TestCompositionComponent = error("lol")
        
        fun <T> inject(): T = getComponent().get<T>()

        val topLevel = {
            {
                {
                    {
                        inject<Foo>()
                    }
                }
            }
        }
    """
    )

    @Test
    fun testMultiCompileGetWithSameName() = multiCodegen(
        listOf(
            source(
                """
                inline fun <T> inject(): T = inject { 
                    compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
                }

                inline fun <C : Any, T> inject(componentProvider: () -> C): T {
                    val component = componentProvider()
                    return component.get()
                }
                
                @CompositionFactory
                fun factory(): TestCompositionComponent {
                    return create()
                }
            """
            )
        ),
        listOf(
            source(
                """
                @Module
                fun fooModule() {
                    installIn<TestCompositionComponent>()
                    transient<Foo>()
                }

                fun invoke() {
                    initializeCompositions()
                    inject<Foo>()
                }
            """,
                name = "File.kt"
            )
        )
    ) {
        it.last().invokeSingleFile()
    }

}
