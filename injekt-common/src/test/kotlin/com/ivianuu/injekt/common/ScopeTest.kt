package com.ivianuu.injekt.common

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.Test

class ScopeTest {

    @Test fun testGetSet() {
        val scope = Scope()
        scope.get<String>(0) shouldBe null
        scope[0] = "value"
        scope.get<String>(0) shouldBe "value"
    }

    @Test fun testMemo() {
        val scope = Scope()
        var calls = 0
        scope(0) { calls++ }
        scope(0) { calls++ }
        scope(1) { calls++ }
        calls shouldBe 2
    }

    @Test fun testDispose() {
        val scope = Scope()
        var disposed = false
        scope[0] = object : Scope.Disposable {
            override fun dispose() {
                disposed = true
            }
        }

        disposed.shouldBeFalse()
        scope.dispose()
        disposed.shouldBeTrue()
    }

}