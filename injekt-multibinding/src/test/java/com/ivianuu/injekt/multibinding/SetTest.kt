package com.ivianuu.injekt.multibinding

import com.ivianuu.injekt.component
import com.ivianuu.injekt.dependencies
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.module
import com.ivianuu.injekt.modules
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

class SetTest {

    @Test
    fun testSetMultiBinding() {
        val component = component {
            modules(
                module {
                    factory("name_one") { "value_one" } bindIntoSet "values"
                    factory("name_two") { "value_two" } bindIntoSet "values"
                    factory("name_three") { "value_three" }
                    bindIntoSet<String>("values", implementationName = "name_three")
                }
            )
        }

        val set = component.getSet<String>("values")

        assertEquals(3, set.size)
        assertTrue(set.contains("value_one"))
        assertTrue(set.contains("value_two"))
        assertTrue(set.contains("value_three"))
    }

    @Test
    fun testOverride() {
        val component1 = component {
            modules(
                module {
                    factory { "my_value" } bindIntoSet "values"
                }
            )
        }

        val component2 = component {
            dependencies(component1)
            modules(
                module {
                    factory { "my_overridden_value" } bindIntoSet SetBinding("values", true)
                }
            )
        }

        assertEquals("my_overridden_value", component2.getSet<String>("values").first())
    }

    @Test
    fun testAllowValidOverride() {
        val component1 = component {
            modules(
                module {
                    factory { "my_value" } bindIntoSet "values"
                }
            )
        }

        val component2 = component {
            dependencies(component1)
            modules(
                module {
                    factory { "my_value" } bindIntoSet SetBinding("values", override = true)
                }
            )
        }

        var throwed = false

        try {
            component2.getSet<String>("values")
        } catch (e: Exception) {
            throwed = true
        }

        assertFalse(throwed)
    }

    /*@Test
    fun testDisallowInvalidOverride() {
        val component1 = component {
            modules(
                module {
                    factory { "my_value" } bindIntoSet "values"
                }
            )
        }

        val component2 = component {
            dependencies(component1)
            modules(
                module {
                    factory { "my_value" } bindIntoSet "values"
                }
            )
        }

        var throwed = false

        try {
            component2.getSet<String>("values")
        } catch (e: Exception) {
            throwed = true
        }

        assertTrue(throwed)
    }*/

}