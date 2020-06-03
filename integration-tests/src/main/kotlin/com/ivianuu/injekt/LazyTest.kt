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

class LazyTest {

    @Test
    fun testLazyOfTransient() = codegen(
        """
        @InstanceFactory
        fun invoke(): @Lazy () -> Foo { 
            transient { Foo() }
            return create()
        }
         """
    ) {
        val lazy = invokeSingleFile<@Lazy () -> Foo>()
        assertSame(lazy(), lazy())
    }

    @Test
    fun testLazyOfScoped() = codegen(
        """
        @InstanceFactory
        fun invoke(): @Lazy () -> Foo { 
            scoped { Foo() }
            return create()
        }
         """
    ) {
        val lazy = invokeSingleFile<@Lazy () -> Foo>()
        assertSame(lazy(), lazy())
    }

    @Test
    fun testQualifiedLazy() = codegen(
        """
            @Target(AnnotationTarget.TYPE)
            @Qualifier
            annotation class TestQualifier1
        @Factory
        fun invoke(): @Lazy () -> @TestQualifier1 Foo { 
            transient<@TestQualifier1 Foo> { Foo() }
            return create()
        }
         """
    ) {
        assertOk()
    }

    @Test
    fun testProviderOfLazy() = codegen(
        """
        interface TestComponent {
            val providerOfLazy: @Provider () -> @Lazy () -> Foo
        }
        
        @Factory
        fun createComponent(): TestComponent { 
            transient { Foo() }
            return create()
        }
        
        val component = createComponent()
        fun invoke() = component.providerOfLazy
    """
    ) {
        val lazyA =
            invokeSingleFile<@Provider () -> @Lazy () -> Foo>()()
        val lazyB =
            invokeSingleFile<@Provider () -> @Lazy () -> Foo>()()
        assertNotSame(lazyA, lazyB)
        assertSame(lazyA(), lazyA())
        assertSame(lazyB(), lazyB())
        assertNotSame(lazyA(), lazyB())
    }

}