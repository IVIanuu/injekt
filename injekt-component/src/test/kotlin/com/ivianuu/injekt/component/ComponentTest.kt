package com.ivianuu.injekt.component

import com.ivianuu.injekt.GivenSetElement
import com.ivianuu.injekt.common.keyOf
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.Test

class ComponentTest {

    @Test
    fun testReturnsExistingValue() {
        val component = ComponentBuilder<TestComponent1>()
            .element("value")
            .build()
        component.get<String>() shouldBe "value"
    }

    @Test
    fun testReturnsNullForNotExistingValue() {
        val component = ComponentBuilder<TestComponent1>().build()
        component.getOrNull(keyOf<String>()) shouldBe null
    }

    @Test
    fun testReturnsFromDependency() {
        val component = ComponentBuilder<TestComponent2>()
            .dependency(
                ComponentBuilder<TestComponent1>()
                    .element("value")
                    .build()
            )
            .build()
        component.get<String>() shouldBe "value"
    }

    @Test fun testGetDependencyReturnsDependency() {
        val dependency = ComponentBuilder<TestComponent1>().build()
        val dependent = ComponentBuilder<TestComponent2>()
            .dependency(dependency)
            .build()
        dependent.get<TestComponent1>() shouldBeSameInstanceAs dependency
    }

    @Test fun testGetDependencyReturnsNullIfNotExists() {
        val dependent = ComponentBuilder<TestComponent2>().build()
        dependent.getOrNull(keyOf<TestComponent1>()) shouldBe null
    }

    @Test
    fun testOverridesDependency() {
        val component = ComponentBuilder<TestComponent2>()
            .dependency(
                ComponentBuilder<TestComponent1>()
                    .element("dependency")
                    .build()
            )
            .element("child")
            .build()
        component.get<String>() shouldBe "child"
    }

    @Test
    fun testInjectedElement() {
        @GivenSetElement val injected = componentElement<TestComponent1, String>("value")
        val component = ComponentBuilder<TestComponent1>().build()
        component.get<String>() shouldBe "value"
    }

    @Test fun testGetSet() {
        val component = ComponentBuilder<TestComponent1>().build()
        component.getScopedValue<String>(0) shouldBe null
        component.setScopedValue(0, "value")
        component.getScopedValue<String>(0) shouldBe "value"
    }

    @Test fun testScope() {
        val component = ComponentBuilder<TestComponent1>().build()
        var calls = 0
        component.scope(0) { calls++ }
        component.scope(0) { calls++ }
        component.scope(1) { calls++ }
        calls shouldBe 2
    }

    @Test fun testDispose() {
        val component = ComponentBuilder<TestComponent1>().build()
        var disposed = false
        component.setScopedValue(
            0,
            object : Component.Disposable {
                override fun dispose() {
                    disposed = true
                }
            }
        )

        disposed.shouldBeFalse()
        component.dispose()
        disposed.shouldBeTrue()
    }

}
