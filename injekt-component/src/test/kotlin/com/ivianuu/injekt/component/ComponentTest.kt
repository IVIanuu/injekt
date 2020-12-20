package com.ivianuu.injekt.component

import com.ivianuu.injekt.GivenSetElement
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.Test

class ComponentTest {

    @Test
    fun testReturnsExistingValue() {
        val key = ComponentKey<String>()
        val component = Component(TestComponent1) {
            element(key, "value")
        }
        component[key] shouldBe "value"
    }

    @Test
    fun testReturnsNullForNotExistingValue() {
        val key = ComponentKey<String>()
        val component = Component(TestComponent1)
        component.getOrNull(key) shouldBe null
    }

    @Test
    fun testReturnsFromDependency() {
        val key = ComponentKey<String>()
        val component = Component(TestComponent2) {
            dependency(
                Component(TestComponent1) {
                    element(key, "value")
                }
            )
        }
        component[key] shouldBe "value"
    }

    @Test fun testGetDependencyReturnsDependency() {
        val dependency = Component(TestComponent1)
        val dependent = Component(TestComponent2) { dependency(dependency) }
        dependent.getDependencyOrNull(TestComponent1) shouldBeSameInstanceAs dependency
    }

    @Test fun testGetDependencyReturnsNullIfNotExists() {
        val dependent = Component(TestComponent1)
        dependent.getDependencyOrNull(TestComponent1) shouldBe null
    }

    @Test
    fun testOverridesDependency() {
        val key = ComponentKey<String>()
        val component = Component(TestComponent2) {
            dependency(
                Component(TestComponent1) {
                    element(key, "dependency")
                }
            )
            element(key, "child")
        }
        component[key] shouldBe "child"
    }

    @Test
    fun testInjectedElement() {
        val testKey = ComponentKey<String>()
        @GivenSetElement val injected = componentElement(TestComponent1, testKey, "value")
        val component = ComponentBuilder(TestComponent1).build()
        component[testKey] shouldBe "value"
    }

}
