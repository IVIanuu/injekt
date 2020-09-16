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

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import org.junit.Test

class ReaderTest {

    @Test
    fun testAccessReaderPropertyInConstructor() = codegen(
        """
            @Reader
            val property get() = ""
            
            abstract class SuperClass(val value: String)
            
            @Reader
            class Impl : SuperClass(property)
        """
    )

    @Test
    fun testReaderVar() = codegen(
        """
            private var _backing = "hello world"
            @Reader
            var mutable: String
                get() = _backing
                set(value) { _backing = value }

            @Reader
            fun usage() {
                println(mutable)
                mutable = "bye"
            }
        """
    )

}