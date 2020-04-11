package com.ivianuu.injekt

import junit.framework.Assert.assertEquals
import org.junit.Test

class ComponentBindingsTest {

    @Test
    fun testImplicitComponentBindings() {
        Injekt.logger = PrintLogger()
        val componentA = Component { scopes(TestScope1) }
        val componentB = Component {
            scopes(TestScope2)
            parents(componentA)
        }

        assertEquals(componentA, componentA.get<Component>())
        assertEquals(
            componentA,
            componentA.get<Component>(qualifier = TestScope1)
        )

        assertEquals(componentB, componentB.get<Component>())
        assertEquals(
            componentB,
            componentB.get<Component>(qualifier = TestScope2)
        )
        assertEquals(
            componentA,
            componentB.get<Component>(qualifier = TestScope1)
        )
    }

}