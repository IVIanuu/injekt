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
import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertTrue
import org.junit.Test

class ReadableTest {

    @Test
    fun testReadableInvocationInReadableAllowed() =
        codegen(
            """
            @Readable fun a() {}
            @Readable fun b() { a() }
        """
        ) {
            assertOk()
        }

    @Test
    fun testReadableInvocationInNonReadableNotAllowed() =
        codegen(
            """
            @Readable fun a() {}
            fun b() { a() }
        """
        ) {
            assertCompileError()
        }

    @Test
    fun testReadableInvocationInNonReadableLambdaIsNotAllowed() =
        codegen(
            """
            val lambda: () -> Unit = {
                func()
            }
            @Readable fun func() {}
        """
        ) {
            assertCompileError()
        }

    @Test
    fun testOpenReadableFails() = codegen(
        """
        open class MyClass {
            @Readable 
            open fun func() {
            }
        }
        """
    ) {
        assertCompileError("final")
    }

    @Test
    fun testSimpleReadable() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        @Readable
        fun func(foo: Foo = given()): Foo {
            return foo
        }
        
        fun init() {
            initializeCompositions()
        }
        
        fun invoke(): Foo {
            init()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.runReading { func() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

}
