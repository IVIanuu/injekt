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
import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertTrue
import org.junit.Test

class EntryPointTest {

    @Test
    fun testSimpleEntryPoint() = codegen(
        """
        interface GetFoo {
            val foo: Foo
        }
        
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient<Foo>()
            entryPoint<GetFoo>()
            return create() 
        }
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return entryPointOf<GetFoo>(component).foo
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testEntryPointInjection() = codegen(
        """
        interface GetFoo {
            val foo: Foo
        }
        
        interface GetGetFoo {
            val getFoo: GetFoo
        }
        
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient<Foo>()
            entryPoint<GetFoo>()
            return create() 
        }
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            val getGetFoo = entryPointOf<GetGetFoo>(component)
            return getGetFoo.getFoo.foo
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

}