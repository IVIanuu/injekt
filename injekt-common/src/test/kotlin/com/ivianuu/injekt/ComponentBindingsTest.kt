package com.ivianuu.injekt

import junit.framework.Assert
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

        Assert.assertEquals(componentA, componentA.get<Component>())
        Assert.assertEquals(
            componentA,
            componentA.get<Component>(qualifier = TestScope1)
        )

        Assert.assertEquals(componentB, componentB.get<Component>())
        Assert.assertEquals(
            componentB,
            componentB.get<Component>(qualifier = TestScope2)
        )
        Assert.assertEquals(
            componentA,
            componentB.get<Component>(qualifier = TestScope1)
        )
    }

}