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

class ClassOfTest {

    @Test
    fun testClassOfFunction() = codegen(
        """
        fun <S : Any> classOfA() {
            val classOf = classOf<S>()
        }
        
        fun <T : Any, V : Any> classOfB() {
            val classOf = classOf<T>()
            classOfA<V>()
        }
        
        fun calling() {
            classOfB<String, Int>()
        }
    """
    )

    @Test
    fun testMultiCompilationClassOfFunction() =
        multiCodegen(
            listOf(
                source(
                    """
                fun <S : Any> classOfA() { 
                    val classOf = classOf<S>()
                }
                """
                )
            ),
            listOf(
                source(
                    """
                fun <T : Any, V : Any> classOfB() { 
                    val classOf = classOf<T>()
                    classOfA<V>() 
                }
            """
                )
            ),
            listOf(
                source(
                    """
                fun callingFunction() { 
                    classOfB<String, Int>() 
                } 
            """
                )
            )
        )

    @Test
    fun testClassOfModuleWithDefaultParameters() =
        codegen(
            """
        fun <S : Any> classOfFunction(
            p0: String = "",
            p1: Int = 0
        ) {
            val classOf = classOf<S>()
        }

        fun callingFunction() {
            classOfFunction<Foo>(p1 = 1)
        }
    """
        )

    @Test
    fun testClassOfInProviderDefinition() = codegen(
        """
        
        @Module
        fun <T : Any> module() {
            transient { classOf<T>() }
        }
        
        @InstanceFactory
        fun invoke(): KClass<Foo> {
            module<Foo>()
            return create()
        }
    """
    )

}
