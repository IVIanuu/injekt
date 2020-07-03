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
            dependency(dependency)
            set<Any>()
            map<String, Any>()
            transient { foo: Foo -> Bar(foo) }
            scoped { bar: Bar -> bar.toString() }
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
        
        @Factory
        fun createInstance(): TestComponent1<String> {
            capturingModule("hello world")
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

        @Factory
        fun createInstance(): TestComponent1<String> {
            capturingModule("hello")
            return create()
        }
    """
    )

    @Test
    fun testBindingWithTypeParameterInInlineModule() =
        codegen(
            """ 
        @Module 
        fun <T> module() {
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
    fun testMultipleCompileNestedModuleInAnnotation() = multiCodegen(
        listOf(
            source(
                """
                    annotation class MyClass {
                        companion object {
                            @Module
                            fun <T> module() {
                                transient<T>()
                            }
                        }
                    }
            """
            )
        ),
        listOf(
            source(
                """
                    annotation class MyClass2 {
                        companion object {
                            @Module
                            fun <T> module() {
                                MyClass.module<T>()
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
                        MyClass2.module<Foo>()
                    } 
                """
            )
        )
    )

}
