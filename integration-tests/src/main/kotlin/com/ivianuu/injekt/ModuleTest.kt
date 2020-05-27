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
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import org.junit.Test

class ModuleTest {

    @Test
    fun testModule() = codegen(
        """
        @Module
        fun module(dependency: Any) {
            scope<TestScope>()
            dependency(dependency)
            set<Any>()
            map<String, Any>()
            instance("hello world")
            transient { foo: Foo ->
                Bar(get())
            }
        }
    """
    )

    @Test
    fun testValueParameterCapturingModule() = codegen(
        """
        @Module
        fun capturingModule(capture: String) {
            transient { capture }
        }
        
        @InstanceFactory
        fun createInstance(): String {
            capturingModule("hello world")
            return create()
        }
    """
    )

    @Test
    fun testTypeParameterCapturingModule() = codegen(
        """
        @Target(AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE) 
        @Qualifier 
        annotation class TestQualifier1
        
        @Module
        fun <T> capturingModule() {
            transient<@TestQualifier1 T> { get<T>() }
        }
        
        @InstanceFactory
        fun createInstance(): @TestQualifier1 String {
            transient { "hello world" }
            capturingModule<String>()
            return create()
        }
    """
    )

    @Test
    fun testLocalDeclarationCapturing() = codegen(
        """
        @Module
        fun capturingModule(greeting: String) {
            val local = greeting + " world"
            transient { local }
        }

        @InstanceFactory
        fun createInstance(): String {
            capturingModule("hello")
            return create()
        }
    """
    )

    @Test
    fun testMultipleModulesWithSameName() = codegen(
        """
        @Module
        fun module() {
        }
        
        @Module
        fun module(p0: String) {
        }
    """
    )

    @Test
    fun testIncludeLocalModule() = codegen(
        """
        @Module
        fun outer() {
            @Module
            fun <T> inner(instance: T) {
                instance(instance)
            }
            
            inner("hello world")
            inner(42)
        }
    """
    )

    @Test
    fun testBindingWithTypeParameterInInlineModule() =
        codegen(
            """ 
        @Module
        inline fun <T> module() {
            transient<T>()
        }
    """
        )

    @Test
    fun testMultipleCompileNestedModule() = multiCodegen(
        listOf(
            source(
                """
                class MyClass {
                    companion object {
                        @Module
                        fun module() {
                        
                        }
                    }
                }
            """
            )
        ),
        listOf(
            source(
                """
                    @Module 
                    fun calling() {
                        MyClass.Companion.module()
                    } 
                """
            )
        )
    )

    @Test
    fun testMultipleCompileNestedWithReceiverModule() = multiCodegen(
        listOf(
            source(
                """
                class MyClass {
                    companion object {
                        @Module
                        fun String.module() {
                        
                        }
                    }
                }
            """
            )
        ),
        listOf(
            source(
                """
                    import MyClass.Companion.module
                    
                    @Module 
                    fun calling() {
                        "hello world".module()
                    } 
                """
            )
        )
    )

}
