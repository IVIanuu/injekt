package com.ivianuu.injekt.component

import com.ivianuu.injekt.GivenSetElement
import io.kotest.matchers.shouldBe
import org.junit.Test

class ComponentTest {

    @Test
    fun testBuilderComponentElement() {
        val testKey = object : Component.Key<String> {}
        val component = ComponentBuilder<TestComponent1>()
            .set(testKey, "value")
            .build()
        component[testKey] shouldBe "value"
    }

    @Test
    fun testInjectedComponentElement() {
        val testKey = object : Component.Key<String> {}
        @GivenSetElement val injected = componentElement(TestComponent1, testKey, "value")
        val component = ComponentBuilder<TestComponent1>().build()
        component[testKey] shouldBe "value"
    }

}