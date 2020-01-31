package com.ivianuu.injekt

import junit.framework.Assert.assertEquals
import org.junit.Test

class ComponentBuilderTest {

    @Test
    fun testInstance() {
        val component = Component {
            instance("string")
            instance(1)
        }

        assertEquals("string", component.get<String>())
        assertEquals(1, component.get<Int>())
    }


    @Test(expected = IllegalStateException::class)
    fun testThrowsIfScopeIsNullWhileDependencyHasScope() {
        val dependency = Component {
            scopes(TestScopeOne)
        }

        Component { dependencies(dependency) }
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsWhenOverridingScope() {
        val dependency = Component {
            scopes(TestScopeOne)
        }

        Component {
            scopes(TestScopeOne)
            dependencies(dependency)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsOnDependenciesWithSameScope() {
        val dependency1 = Component {
            scopes(TestScopeOne)
        }

        val dependency2 = Component {
            scopes(TestScopeOne)
        }

        Component {
            scopes(TestScopeTwo)
            dependencies(dependency1, dependency2)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testDisallowsNestedImplicitOverride() {
        val componentA = Component {
            modules(
                Module {
                    factory { "my_value" }
                }
            )
        }

        Component {
            dependencies(componentA)
            modules(
                Module {
                    factory { "my_overridden_value" }
                }
            )
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsIfDependenciesOverrideEachOther() {
        val dependency1 = Component {
            modules(
                Module {
                    factory { "value_1" }
                }
            )
        }

        val dependency2 = Component {
            modules(
                Module {
                    factory { "value_2" }
                }
            )
        }

        Component { dependencies(dependency1, dependency2) }
    }

}