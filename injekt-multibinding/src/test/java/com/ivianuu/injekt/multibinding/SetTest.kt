package com.ivianuu.injekt.multibinding

import com.ivianuu.injekt.component
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.module
import com.ivianuu.injekt.modules
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test

class SetTest {

    @Test
    fun testSetMultiBinding() {
        val component = component {
            modules(
                module {
                    factory("name_one") { "value_one" } bindIntoSet SetBinding("values")
                    factory("name_two") { "value_two" } bindIntoSet SetBinding("values")
                    factory("name_three") { "value_three" }
                    bindIntoSet<String>(SetBinding("values"), "name_three")
                }
            )
        }

        val set = component.getSet<String>("values")

        assertEquals(3, set.size)
        assertTrue(set.contains("value_one"))
        assertTrue(set.contains("value_two"))
        assertTrue(set.contains("value_three"))
    }

}