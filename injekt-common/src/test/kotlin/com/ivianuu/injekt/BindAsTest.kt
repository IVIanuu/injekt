package com.ivianuu.injekt

import com.ivianuu.injekt.synthetic.BindAs
import com.ivianuu.injekt.synthetic.Factory
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNull
import org.junit.Test

class BindAsTest {

    @Test
    fun testBindAs() {
        val component = Component {
            single(behavior = BindAs(keyOf<Any>())) { TestDep1() }
        }

        assertNull(component.get<TestDep1?>())
        assertNotNull(component.get<Any>())
    }

    @Test
    fun testBindAsAnnotation() {
        val component = Component()
        assertNotNull(component.get<Any>())
    }

    @BindAs(KeyOf<Any>())
    @Factory
    class BindAsDep

}
