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

package com.ivianuu.injekt.frontend

import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import org.junit.Test

class MapDslTest {

    @Test
    fun testSupportedMapKeyType() = codegen(
        """
        @Module
        fun test() { 
            map<String, Any>()
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testUnsupportedMapKeyType() = codegen(
        """
        @Module
        fun test() { 
            map<Any, Any>()
        }
    """
    ) {
        assertCompileError("map key")
    }

    @Test
    fun testConstantMapKey() = codegen(
        """
        @Module
        fun test() { 
            map<KClass<*>, Any> {
                put<String>(String::class)
            }
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testDynamicMapKey() = codegen(
        """
        fun key() = String::class
        @Module
        fun test() { 
            map<KClass<*>, Any> {
                put<String>(key())
            }
        }
    """
    ) {
        assertCompileError("constant")
    }

}
