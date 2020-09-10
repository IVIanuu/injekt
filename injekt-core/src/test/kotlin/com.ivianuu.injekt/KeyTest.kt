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

import androidx.compose.runtime.Composable
import org.junit.Assert.assertNotEquals
import org.junit.Test

class KeyTest {

    @Test
    fun testDistinctsNullability() {
        val a = keyOf<String>()
        val b = keyOf<String?>()
        assertNotEquals(b, a)
    }

    @Test
    fun testDistinctsTypeAlias() {
        val a = keyOf<String>()
        val b = keyOf<StringAlias>()
        assertNotEquals(b, a)
    }

    @Test
    fun testDistinctsTypeArguments() {
        val a = keyOf<List<String>>()
        val b = keyOf<List<Int>>()
        assertNotEquals(b, a)
    }

    @Test
    fun testDistinctsSuspend() {
        val a = keyOf<suspend () -> Unit>()
        val b = keyOf<() -> Unit>()
        assertNotEquals(b, a)
    }

    @Test
    fun testDistinctsReader() {
        val a = keyOf<@Reader () -> Unit>()
        val b = keyOf<() -> Unit>()
        assertNotEquals(b, a)
    }

    @Test
    fun testDistinctsComposable() {
        val a = keyOf<@Composable () -> Unit>()
        val b = keyOf<() -> Unit>()
        assertNotEquals(b, a)
    }

    @Test
    fun testDistinctsComposableReader() {
        val a = keyOf<@Reader @Composable () -> Unit>()
        val b = keyOf<@Composable () -> Unit>()
        assertNotEquals(b, a)
    }

}

typealias StringAlias = String
