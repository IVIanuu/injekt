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

package com.ivianuu.injekt.common

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import org.junit.Test

class KeyInfoTest {

    @Test
    fun testSimpleKeyInfo() {
        val keyInfo = keyInfoOf<String>()
        assertEquals(String::class, keyInfo.classifier)
        assertEquals(0, keyInfo.arguments)
        assertFalse(keyInfo.isMarkedNullable)
    }

    @Test
    fun testNullableKeyInfo() {
        val keyInfo = keyInfoOf<String?>()
        assertEquals(String::class, keyInfo.classifier)
        assertEquals(0, keyInfo.arguments)
        assertFalse(keyInfo.isMarkedNullable)
    }

    @Test
    fun testKeyInfoWithTypeArguments() {
        val keyInfo = keyInfoOf<List<String>>()
        assertEquals(keyInfo.classifier, List::class)
        assertEquals(1, keyInfo.arguments)
        assertFalse(keyInfo.isMarkedNullable)
        val elementKeyInfo = keyInfo.arguments.single()
        assertEquals(String::class, elementKeyInfo.classifier)
        assertEquals(0, elementKeyInfo.arguments)
        assertFalse(elementKeyInfo.isMarkedNullable)
    }

}
