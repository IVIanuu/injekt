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
import io.kotest.matchers.*
import io.kotest.matchers.nulls.*
import org.junit.*
import kotlin.reflect.*

class CommonGivensTest {
    @Test
    fun testCanUseMapForSetOfPairs() {
        @Given val elements = setOf("key" to "value")
        val map = givenOrNull<Map<String, String>>()
        map.shouldNotBeNull()
        map.size shouldBe 1
        map["key"] shouldBe "value"
    }

    @Test
    fun testCanUseLazy() {
        givenOrNull<Lazy<Foo>>().shouldNotBeNull()
    }

    @Test
    fun testCanUseKClass() {
        givenOrNull<KClass<Foo>>().shouldNotBeNull()
    }

    @Test
    fun testCanUseType() {
        givenOrNull<TypeKey<Foo>>().shouldNotBeNull()
    }

    @Given
    private class Foo
}
