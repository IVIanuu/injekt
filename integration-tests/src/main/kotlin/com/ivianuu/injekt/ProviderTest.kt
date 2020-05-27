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
import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertSame
import org.junit.Test

class ProviderTest {

    @Test
    fun testProviderOfTransient() = codegen(
        """
        interface TestComponent {
            val provider: @Provider () -> Foo
        }
        
        @Factory
        fun createComponent(): TestComponent { 
            transient { Foo() }
            return create()
        }
        
        fun invoke() = createComponent().provider
    """
    ) {
        val provider =
            invokeSingleFile<@Provider () -> Foo>()
        assertNotSame(provider(), provider())
    }

    @Test
    fun testProviderOfScoped() = codegen(
        """
        interface TestComponent {
            val provider: @Provider () -> Foo
        }
        
        @Factory
        fun createComponent(): TestComponent { 
            scoped { Foo() }
            return create()
        }
        
        fun invoke() = createComponent().provider
    """
    ) {
        val provider =
            invokeSingleFile<@Provider () -> Foo>()
        assertSame(provider(), provider())
    }

    @Test
    fun testQualifiedProvider() = codegen(
        """
            @Target(AnnotationTarget.TYPE)
            @Qualifier
            annotation class TestQualifier1
        @Factory
        fun invoke(): @Provider () -> @TestQualifier1 Foo { 
            transient<@TestQualifier1 Foo> { Foo() }
            return create()
        }
         """
    ) {
        assertOk()
    }

}
