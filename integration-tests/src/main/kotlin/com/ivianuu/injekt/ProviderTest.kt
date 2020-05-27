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

class ProviderTest {

    @Test
    fun testProviderFunction() = codegen(
        """
        @Provider
        fun getFoo(): Foo = getGeneric()
        
        @Provider
        fun <T> getGeneric(): T = get()
        
        @InstanceFactory
        fun invoke(): Bar {
            transient<Foo>()
            transient { Bar(getFoo()) }
            return create()
        }
    """
    )

    @Test
    fun testProviderFunctionWithLambda() = codegen(
        """
        @Provider
        fun <T> something(block: @Provider () -> T): T { 
            return block()
        }
        
        @InstanceFactory
        fun invoke(): Bar {
            transient<Foo>()
            transient { something { Bar(get()) } }
            return create()
        }
    """
    )

    /*@Test // todo
    fun testProviderFunctionWithLambdaProperty() = codegen("""
        val lambdaProperty: @Provider () -> Bar = { Bar(get()) }
        
        @Provider
        fun <T> something(block: @Provider () -> T): T {
            return block()
        }
        
        @InstanceFactory
        fun invoke(): Bar {
            transient<Foo>()
            transient { something(lambdaProperty) }
            return create()
        }
    """)*/

}
