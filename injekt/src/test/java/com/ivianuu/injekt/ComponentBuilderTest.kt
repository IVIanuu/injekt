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

class ComponentBuilderTest {

    @Test
    fun testOverride() {
        val component = component {
            module {
                factory { "my_value" }
                single(override = true) { "my_overridden_value" }
            }
        }

        assertEquals("my_overridden_value", component.get<String>())
    }

    @Test
    fun testAllowsValidOverride() {
        var throwed = false

        try {
            component {
                module {
                    factory { "my_value" }
                    single(override = true) { "my_overridden_value" }
                }
            }
        } catch (e: Exception) {
            throwed = true
        }

        assertFalse(throwed)
    }

    @Test
    fun testDisallowsInvalidOverride() {
        var throwed = false

        try {
            component {
                module {
                    factory { "my_value" }
                    single { "my_overridden_value" }
                }
            }
        } catch (e: Exception) {
            throwed = true
        }

        assertTrue(throwed)
    }

    @Test
    fun testNestedOverride() {
        val component1 = component {
            module {
                factory { "my_value" }
            }
        }

        val component2 = component {
            dependencies(component1)
            module {
                single(override = true) { "my_overridden_value" }
            }
        }

        assertEquals("my_value", component1.get<String>())
        assertEquals("my_overridden_value", component2.get<String>())
    }

    @Test
    fun testAllowsNestedValidOverride() {
        val component1 = component {
            module {
                factory { "my_value" }
            }
        }

        var throwed = false

        try {
            component {
                dependencies(component1)
                module {
                    single(override = true) { "my_overridden_value" }
                }
            }
        } catch (e: Exception) {
            throwed = true
        }

        assertFalse(throwed)
    }

    @Test
    fun testDisallowsNestedInvalidOverride() {
        val component1 = component {
            module {
                factory { "my_value" }
            }
        }

        var throwed = false

        try {
            component {
                dependencies(component1)
                module {
                    single { "my_overridden_value" }
                }
            }
        } catch (e: Exception) {
            throwed = true
        }

        assertTrue(throwed)
    }

}