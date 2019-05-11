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

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

class AttributesTest {

    @Test
    fun testGetAndSet() {
        val attrs = attributesOf()

        assertFalse(attrs.contains("key"))

        attrs["key"] = "test"

        assertTrue(attrs.contains("key"))

        val value = attrs.get<String>("key")
        assertEquals("test", value)
    }

    @Test
    fun testGetIfNotSet() {
        val attrs = attributesOf()
        assertFalse(attrs.contains("key"))
        assertTrue(attrs.getOrNull<String>("key") == null)
    }

    @Test
    fun testOverridesValues() {
        val attrs = attributesOf()

        attrs["key"] = "value1"
        attrs["key"] = "value2"

        val value = attrs.get<String>("key")
        assertEquals("value2", value)
    }
}