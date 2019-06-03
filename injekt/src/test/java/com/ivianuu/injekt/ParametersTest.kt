/*
 * Copyright 2018 Manuel Wrage
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

import org.junit.Assert.assertEquals
import org.junit.Test

class ParametersTest {

    @Test
    fun testGetParams() {
        val myString = "empty"
        val myInt = 42
        val params = parametersOf(myString, myInt)

        val (s: String, i: Int) = params
        assertEquals(myString, s)
        assertEquals(myInt, i)
    }

    @Test
    fun testSize() {
        val params = parametersOf(1, 2, 3)
        assertEquals(3, params.size)
    }


}