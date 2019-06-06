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

import junit.framework.Assert.assertFalse
import org.junit.Test

class TypeTest {

    @Test
    fun testParameterDistinction() {
        val listOfInts = typeOf<List<Int>>()
        val listOfStrings = typeOf<List<String>>()
        assertFalse(listOfInts == listOfStrings)
    }

    @Test
    fun testNullableDistinction() {
        val stringType = typeOf<String>()
        val nullableStringType = typeOf<String?>()
        assertFalse(stringType == nullableStringType)
    }

}