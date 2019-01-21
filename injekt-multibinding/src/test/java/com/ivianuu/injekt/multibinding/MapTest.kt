package com.ivianuu.injekt.multibinding

import com.ivianuu.injekt.component
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.module
import com.ivianuu.injekt.modules
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

class MapTest {

    @Test
    fun testMapMultiBinding() {
        val component = component {
            modules(
                module {
                    factory("name_one") { "value_one" } bindIntoMap ("values" to "key_one")
                    factory("name_two") { "value_two" } bindIntoMap ("values" to "key_two")
                    factory("name_three") { "value_three" }
                    bindIntoMap<String>("values", "key_three", implementationName = "name_three")
                }
            )
        }

        val map = component.getMap<String, String>("values")

        assertEquals(3, map.size)
        assertEquals(map["key_one"], "value_one")
        assertEquals(map["key_two"], "value_two")
        assertEquals(map["key_three"], "value_three")
    }

    @Test
    fun testOverride() {
        val component = component {
            modules(
                module {
                    factory("name_one") { "value_one" } bindIntoMap ("values" to "key_one")
                    factory("name_two") { "value_two" } bindIntoMap MapBinding(
                        "values",
                        "key_one",
                        true
                    )
                }
            )
        }

        assertEquals("value_two", component.getMap<String, String>("values")["key_one"])
    }

    @Test
    fun testAllowValidOverride() {
        val component = component {
            modules(
                module {
                    factory("name_one") { "value_one" } bindIntoMap ("values" to "key")
                    factory("name_two") { "value_two" } bindIntoMap MapBinding(
                        "values",
                        "key",
                        true
                    )
                }
            )
        }

        var throwed = false

        try {
            component.getMap<String, String>("values")
        } catch (e: Exception) {
            throwed = true
        }

        assertFalse(throwed)
    }

    @Test
    fun testDisallowInvalidOverride() {
        val component = component {
            modules(
                module {
                    factory("name_one") { "value_one" } bindIntoMap ("values" to "key")
                    factory("name_two") { "value_two" } bindIntoMap ("values" to "key")
                }
            )
        }

        var throwed = false

        try {
            component.getMap<String, String>("values")
        } catch (e: Exception) {
            throwed = true
        }

        assertTrue(throwed)
    }
}