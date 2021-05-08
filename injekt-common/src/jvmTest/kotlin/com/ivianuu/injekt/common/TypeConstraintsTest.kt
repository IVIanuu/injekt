/*
 * Copyright 2021 Manuel Wrage
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

package com.ivianuu.injekt.common

import com.ivianuu.injekt.*
import io.kotest.matchers.nulls.*
import org.junit.*

class TypeConstraintsTest {
    @Test
    fun testIsEqualWithMatchingType() {
        givenOrNull<IsEqual<String, String>>().shouldNotBeNull()
    }

    @Test
    fun testIsEqualWithNotMatchingType() {
        givenOrNull<IsEqual<CharSequence, String>>().shouldBeNull()
    }

    @Test
    fun testIsSubTypeWithMatchingType() {
        givenOrNull<IsSubType<String, CharSequence>>().shouldNotBeNull()
    }

    @Test
    fun testIsSubTypeWithNotMatchingType() {
        givenOrNull<IsSubType<Int, CharSequence>>().shouldBeNull()
    }

    @Test
    fun testIsNotSubTypeWithMatchingType() {
        givenOrNull<IsNotSubType<String, CharSequence>>().shouldBeNull()
    }

    @Test
    fun testIsNotSubTypeWithNotMatchingType() {
        givenOrNull<IsNotSubType<Int, CharSequence>>().shouldNotBeNull()
    }

    @Test
    fun testIsNotEqualWithMatchingType() {
        givenOrNull<IsNotEqual<String, String>>().shouldBeNull()
    }

    @Test
    fun testIsNotEqualWithNotMatchingType() {
        givenOrNull<IsNotEqual<CharSequence, String>>().shouldNotBeNull()
    }
}
