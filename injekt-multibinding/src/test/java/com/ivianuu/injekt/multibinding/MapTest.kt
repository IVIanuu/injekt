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
                    factory(NameOne) { "value_one" } bindIntoMap (Values to "key_one")
                    factory(NameTwo) { "value_two" } bindIntoMap (Values to "key_two")
                    factory(NameThree) { "value_three" }
                    bindIntoMap<String>(Values, "key_three", implementationQualifier = NameThree)
                }
            )
        }

        val map = component.getMap<String, String>(Values)

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
                    factory(NameOne) { "value_one" } bindIntoMap (Values to "key_one")
                    factory(NameTwo) { "value_two" } bindIntoMap MapBinding(
                        Values,
                        "key_one",
                        true
                    )
                }
            )
        }

        assertEquals("value_two", component.getMap<String, String>(Values)["key_one"])
    }

    @Test
    fun testAllowValidOverride() {
        val component = component {
            modules(
                module {
                    factory(NameOne) { "value_one" } bindIntoMap (Values to "key")
                    factory(NameTwo) { "value_two" } bindIntoMap MapBinding(
                        Values,
                        "key",
                        true
                    )
                }
            )
        }

        var throwed = false

        try {
            component.getMap<String, String>(Values)
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
                    factory(NameOne) { "value_one" } bindIntoMap (Values to "key")
                    factory(NameTwo) { "value_two" } bindIntoMap (Values to "key")
                }
            )
        }

        var throwed = false

        try {
            component.getMap<String, String>(Values)
        } catch (e: Exception) {
            throwed = true
        }

        assertTrue(throwed)
    }
}