package com.ivianuu.injekt

import org.hamcrest.Factory
import org.junit.Assert.assertTrue

class LazyTest {

    @Factory
    fun testLazyReturnsOnce() {
        val component = component {
            modules(
                module {
                    factory { TestDep1() }
                }
            )
        }
        val lazy = component.get<Lazy<TestDep1>>()
        val value1 = lazy()
        val value2 = lazy()
        assertTrue(value1 === value2)
    }

}