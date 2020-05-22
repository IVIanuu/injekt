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

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert.assertEquals
import org.junit.Test

class InstanceFactoryTest {

    @Test
    fun testCreateInstance() = codegen(
        """
        @InstanceFactory
        fun createBar(): Bar { 
            transient<Foo>()
            transient<Bar>()
            return create()
        }
        
        fun invoke() = createBar()
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testCreateInstanceAdvanced() = codegen(
        """
        @TestScope 
        class MyClass(foo: Foo, bar: Bar)
        @InstanceFactory
        fun createBar(): MyClass {
            scope<TestScope>()
            scoped<Foo>()
            transient<Bar>()
            return create()
        }
        
        fun invoke() = createBar()
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testDependencyInInstance() = codegen(
        """
        interface FooOwner {
            val foo: Foo
        }
        
        @InstanceFactory
        fun createFoo(fooOwner: FooOwner): Foo {
            dependency(fooOwner)
            return create()
        }
    """
    )

    @Test
    fun testLocalFunctionInstanceFactory() = codegen(
        """
        fun createInstance(): Bar {
            @InstanceFactory
            fun factory(): Bar {
                transient<Foo>()
                transient<Bar>()
                return create()
            }
            return factory()
        }
    """
    )

    @Test
    fun testInstanceFactoryLambda() = codegen(
        """
        fun createInstance(): Bar {
            val factory = @InstanceFactory {
                transient<Foo>()
                transient<Bar>()
                create<Bar>()
            }
            return factory()
        }
    """
    )

    @Test
    fun testInlineFactory() = codegen(
        """
        @InstanceFactory
        inline fun <T> createInstance(): T {
            transient<Foo>()
            transient<Bar>()
            return create()
        }
        
        fun invoke() {
            createInstance<Foo>()
            createInstance<Bar>()
        }
    """
    )

    @Test
    fun testMultiCompilationInlineFactory() = multiCodegen(
        listOf(
            source(
                """
                    @InstanceFactory 
                    inline fun <T> createInstance(): T { 
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
                        createInstance<Foo>()
                        createInstance<Bar>()
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
                    @InstanceFactory
                    inline fun <T> createInstance(): T { 
                        transient<Foo>()
                        transient<Bar>()
                        return create()
                    }
                    
                    @InstanceFactory 
                    inline fun <T> createInstance(block: @Module () -> Unit): T {
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
                        createInstance<Foo>()
                        createInstance<Bar> {
                            transient<Foo>()
                            transient<Bar>()
                        }
                    }
                """
            )
        )
    )

    @Test
    fun testFactoryAsMemberFunction() = codegen(
        """
        class MyClass {
            val outerField = ""
            
            @Transient class Dep(myClass: MyClass, foo: Foo)
            @InstanceFactory
            fun createComponent(userId: String): Dep {
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
    fun testFactoryAsExtensionFunction() = codegen(
        """
        class MyClass {
            val outerField = "" 
            @Transient class Dep(myClass: MyClass, foo: Foo)
        }
        
        @InstanceFactory 
        fun MyClass.createComponent(userId: String): MyClass.Dep { 
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
    fun testFactoryWithDefaultParameters() = codegen(
        """
        @InstanceFactory
        fun createInstance(string: String = "default"): String {
            instance(string)
            return create()
        }
        
        fun invoke() = createInstance() to createInstance("non_default")
    """
    ) {
        val pair = invokeSingleFile<Pair<String, String>>()
        assertEquals("default", pair.first)
        assertEquals("non_default", pair.second)
    }

}