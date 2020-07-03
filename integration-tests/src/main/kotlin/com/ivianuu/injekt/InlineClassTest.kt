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
import org.junit.Test

class InlineClassTest {

    @Test
    fun testInlineClassInDsl() = codegen(
        """
        inline class InlineFoo(val foo: Foo)
        
        @Factory
        fun factory(): TestComponent1<InlineFoo> {
            transient<Foo>()
            transient { InlineFoo(get()) }
            return create()
        }
    """
    )

    @Test
    fun testAnnotatedInlineClass() = codegen(
        """
        @Transient
        inline class InlineFoo(val foo: Foo)
 
        @Factory
        fun factory(): TestComponent1<InlineFoo> {
            transient<Foo>()
            return create()
        }
    """
    )

}