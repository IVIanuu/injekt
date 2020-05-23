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

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test

class InlineTest {

    @Test
    fun testInlineModuleLambda() = codegen(
        """
        @InstanceFactory
        inline fun <T> buildInstance(block: @Module () -> Unit): T { 
            block()
            return create() 
        }
        
        fun invoke() = buildInstance<Foo> { transient<Foo>() }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testNestedInlineModuleLambda() = codegen(
        """
        @InstanceFactory
        inline fun <T> buildInstance(block: @Module () -> Unit): T {
            nested(block)
            return create() 
        }
        
        @Module
        inline fun nested(block: @Module () -> Unit) {
            block()
        }
        
        fun invoke() = buildInstance<Foo> { transient<Foo>() }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testInlineModuleLambdaWithArgs() = codegen(
        """
        @Module
        fun calling() {
            inlined { instance(it) }
        }
        
        @Module
        inline fun inlined(block: @Module (String) -> Unit) {
            block("hello world")
        }
        
        @InstanceFactory
        fun invoke(): String {
            calling()
            return create()
        }
    """
    ) {
        assertEquals("hello world", invokeSingleFile())
    }

    @Test
    fun testNestedInlineModuleLambdaWithArgs() = codegen(
        """
        @Module
        fun calling() {
            inlined { instance(it) }
        }
        
        @Module
        inline fun inlined(block: @Module (String) -> Unit) {
            nestedInlined(block)
        }
        
        @Module
        inline fun nestedInlined(block: @Module (String) -> Unit) {
            block("hello world")
        }
        
        @InstanceFactory
        fun invoke(): String {
            calling()
            return create()
        }
    """
    ) {
        assertEquals("hello world", invokeSingleFile())
    }

    @Test
    fun testFactoryWithModuleParam() = codegen(
        """
        @InstanceFactory
        inline fun factory(block: @Module () -> Unit): Foo {
            block()
            return create()
        }
        
        fun invoke() = factory { 
            transient<Foo>() 
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testFactoryWithModuleParamInClass() = codegen(
        """
        class Lol {
            @InstanceFactory
            inline fun factory(block: @Module () -> Unit): Foo {
                block()
                return create()
            }
        }
        
        fun invoke() = Lol().factory { 
            transient<Foo>() 
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testCapturingModuleLambda() = codegen(
        """
        interface TestComponent<T> {
            val dep: T
        }
        @Factory
        inline fun <T> createComponent(block: @Module () -> Unit): TestComponent<T> {
            block()
            return create()
        }
        
        class MyClass {
            init {
                createComponent<MyClass> {
                    instance(this)
                }
            }
        }
    """
    )

    @Test
    fun testCapturingLocalModule() = codegen(
        """
        class MyClass {
            init {
                @Module
                fun localModule() {
                    instance(this)
                }
                @InstanceFactory
                fun createComponent(): MyClass {
                    localModule()
                    return create()
                }
            }
        }
    """
    )

    @Test
    fun testCapturingInnerClassLocalModule() = codegen(
        """
        interface TestComponent {
            val outer: OuterClass
            val inner: OuterClass.InnerClass
        }
        class OuterClass {
            inner class InnerClass {
                init { 
                    @Module 
                    fun localModule() { 
                        instance(this@OuterClass)
                        transient { this@InnerClass }
                    } 
                    @Factory 
                    fun createComponent(): TestComponent { 
                        localModule()
                        return create() 
                    } 
                }   
            }
        }
    """
    )

    @Test
    fun testSimpleInlineModule() = codegen(
        """
        @Module 
        inline fun <T : Any, S> inlinedModule(definition: @ProviderDsl (AssistedParameters) -> S) {
            transient<T>()
            inlinedModule2(definition)
        }
        
        @Module 
        inline fun <P> inlinedModule2(definition: @ProviderDsl (AssistedParameters) -> P) {
            transient(definition)
        }
        
        @InstanceFactory
        fun factory(): Bar { 
            inlinedModule<Foo, Bar> { Bar(get()) }
            return create()
        }
    """
    )

    @Test
    fun testCapturingMemberFunction() = codegen(
        """
        class MyClass {
            @InstanceFactory
            fun factory(): MyClass { 
                transient { this@MyClass }
                return create()
            }
        }
    """
    )

    @Test
    fun testCapturingMemberAndExtensionFunction() =
        codegen(
            """
        class MyClass {
            @InstanceFactory
            fun String.factory(): Pair<MyClass, String> { 
                transient { this@MyClass to this@factory }
                return create()
            }
        }
    """
        )

    @Test
    fun testCapturingMemberAndExtensionInLocalFunction() =
        codegen(
            """
        class MyClass {
            @InstanceFactory
            fun String.factory(): Pair<MyClass, String> { 
                @Module
                fun local() {
                    transient { this@MyClass to this@factory }
                }
                local()
                return create()
            }
        }
    """
        )

    @Test
    fun testInlineModuleWithTypeParameters() =
        multiCodegen(
            listOf(
                source(
                    """
                    @Qualifier
                    @Target(AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
                    annotation class TestQualifier1
        class Context {
            fun <T : Any> getSystemService(clazz: Class<T>): T = error("not implemented")
        }
        
        object ContextCompat { 
            fun <T : Any> getSystemService(context: Context, clazz: Class<T>): T = context.getSystemService(clazz)
        }
        
        @Module
        inline fun <T : Any> systemService() {
            val clazz = classOf<T>()
            transient<T> {
                ContextCompat.getSystemService(
                    get<@TestQualifier1 Context>(),
                    clazz.java
                )
            }
        }

        @Module
        fun systemServices() {
            systemService<Foo>()
            systemService<Bar>()
        }
        
        
    """
                )
            ),
            listOf(
                source(
                    """
        @InstanceFactory
        fun createComponent(): Bar {
            @TestQualifier1 transient { Context() }
            systemServices()
            return create()
        }
    """
                )
            )
        )

}
