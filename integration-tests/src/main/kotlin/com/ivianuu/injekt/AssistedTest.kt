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
import org.junit.Test

class AssistedTest {

    @Test
    fun testAssistedWithAnnotations() = codegen(
        """
        @Unscoped
        class Dep(
            @Assisted val assisted: String,
            val foo: Foo
        )
        
        @Factory
        fun factory(): TestComponent1<@Provider (String) -> Dep> {
            unscoped<Foo>()
            return create()
        }
        
        fun invoke() {
            val depFactory = factory().a
            val result: Dep = depFactory("hello world")
        }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testMultiCompileAssistedWithAnnotations() = multiCodegen(
        listOf(
            source(
                """
                @Unscoped 
                class Dep(
                    @Assisted val assisted: String,
                    val foo: Foo
                )
                """
            )
        ),
        listOf(
            source(
                """
                @Factory 
                fun factory(): TestComponent1<@Provider (String) -> Dep> { 
                    unscoped { Foo() }
                    return create()
                }
                    
                fun invoke() {
                    val depFactory = factory().a
                    val result = depFactory("hello world")
                }
                """,
                name = "File.kt"
            )
        )
    ) {
        it.last().invokeSingleFile()
    }


    @Test
    fun testAssistedInDsl() = codegen(
        """
        @Factory
        fun factory(): TestComponent1<@Provider (Foo) -> Bar> {
            unscoped { foo: Foo -> Bar(foo) }
            return create()
        }
        
        fun invoke() {
            val barFactory = factory().a
            val bar: Bar = barFactory(Foo())
        }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testMultiCompileAssistedInDsl() = multiCodegen(
        listOf(
            source(
                """
                @Module 
                fun assistedModule() { 
                    unscoped { foo: Foo -> Bar(foo) }
                }
                """
            )
        ),
        listOf(
            source(
                """
                @Factory 
                fun factory(): TestComponent1<@Provider (Foo) -> Bar> {
                    assistedModule()
                    return create()
                }
                
                fun invoke() = factory().a
                """,
                name = "File.kt"
            )
        )
    ) {
        it.last().invokeSingleFile()
    }

}
