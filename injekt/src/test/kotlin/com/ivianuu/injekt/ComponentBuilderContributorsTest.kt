package com.ivianuu.injekt

import junit.framework.Assert.assertEquals
import org.junit.Test

class ComponentBuilderContributorsTest {

    @Test
    fun testOrdering() {
        val existingContributors = ComponentBuilderContributors.allContributors.toList()
        ComponentBuilderContributors.allContributors.clear()

        val initA = object : ComponentBuilderContributor {
            override val invokeOnInit: Boolean
                get() = true

            override fun apply(builder: ComponentBuilder) {
            }
        }
        val initB = object : ComponentBuilderContributor {
            override val invokeOnInit: Boolean
                get() = true

            override fun apply(builder: ComponentBuilder) {
            }
        }
        val nonInitA = object : ComponentBuilderContributor {
            override val invokeOnInit: Boolean
                get() = false

            override fun apply(builder: ComponentBuilder) {
            }
        }
        val nonInitB = object : ComponentBuilderContributor {
            override val invokeOnInit: Boolean
                get() = false

            override fun apply(builder: ComponentBuilder) {
            }
        }

        Injekt {
            componentBuilderContributors(
                initA, nonInitA, nonInitB, initB
            )
        }

        assertEquals(listOf(initA, initB, nonInitA, nonInitB), ComponentBuilderContributors.get())
        ComponentBuilderContributors.allContributors += existingContributors
    }

}
