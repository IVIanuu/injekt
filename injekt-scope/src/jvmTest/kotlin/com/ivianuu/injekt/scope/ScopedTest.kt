package com.ivianuu.injekt.scope

import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.*
import io.kotest.matchers.*
import org.junit.*

class ScopedTest {
    @Qualifier
    annotation class Element
    @Test
    fun testScoped() {
        var callCount = 0
        class Foo
        @Given
        fun scopedFoo(): @Scoped<TestGivenScope1> Foo {
            callCount++
            return Foo()
        }
        @Given
        fun fooElement(
            @Given foo: Foo
        ): @InstallElement<TestGivenScope1> @Element Foo = foo
        val scope = given<TestGivenScope1>()
        callCount shouldBe 0
        scope.element<@Element Foo>()
        callCount shouldBe 1
        scope.element<@Element Foo>()
        callCount shouldBe 1
    }
}