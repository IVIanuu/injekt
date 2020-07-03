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
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert.assertTrue
import org.junit.Test

class InlineTest {

    @Test
    fun testInlineModuleWithTypeParameters() =
        multiCodegen(
            listOf(
                source(
                    """
                    class Context { 
                        fun <T : Any> getSystemService(clazz: Class<T>): T = error("not implemented")
                    }
        
                    object ContextCompat { 
                        fun <T : Any> getSystemService(context: Context, clazz: Class<T>): T = context.getSystemService(clazz)
                    }
        
                    @Module
                    inline fun <reified T : Any> systemService() {
                        transient<T> {
                        ContextCompat.getSystemService(get<@TestQualifier1 Context>(), T::class.java)
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
                    @Factory
                    fun factory(): TestComponent1<Bar> {
                        transient<@TestQualifier1 Context> { Context() }
                        systemServices()
                        return create()
                    }
                    """
                )
            )
        )

    @Test
    fun testNestedInlineModuleWithTypeParameters() =
        multiCodegen(
            listOf(
                source(
                    """
                    class Context { 
                        fun <T : Any> getSystemService(clazz: Class<T>): T = error("not implemented")
                    }
        
                    object ContextCompat { 
                        fun <T : Any> getSystemService(context: Context, clazz: Class<T>): T = context.getSystemService(clazz)
                    }
        
                    @Module
                    inline fun <reified T : Any> baseSystemService() {
                        transient<T> {
                            ContextCompat.getSystemService(get<@TestQualifier1 Context>(), T::class.java)
                        }
                }
                """
                )
            ),
            listOf(
                source(
                    """
                    @Module
                    inline fun <reified T : Any> systemService() {
                        baseSystemService<T>()
                    }
                    """
                )
            ),
            listOf(
                source(
                    """
                    @Module 
                    fun systemServices() { 
                        systemService<Foo>()
                        systemService<Bar>()
                    }
                    
                    @Factory
                    fun factory(): TestComponent1<Bar> {
                        transient<@TestQualifier1 Context> { Context() }
                        systemServices()
                        return create()
                    }
                    """
                )
            )
        )

    // todo @Test
    fun testModuleWithGenericFunctionParameter() = codegen(
        """ 
        @Module 
        fun <T, @Reader F : Function<T>> generic(provider: @Reader F) {
            transient(provider)
        }
        
        @Factory
        fun factory(): TestComponent1<Bar> {
            generic { Foo() }
            generic { Bar(get<Foo>()) }
            return create()
        }
        
        fun invoke() = factory().a
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

}
