package com.ivianuu.injekt.component

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.Test

class StorageTest {

    @Test fun testGetSet() {
        val storage = Storage<TestComponent1>()
        storage.get<String>(0) shouldBe null
        storage[0] = "value"
        storage.get<String>(0) shouldBe "value"
    }

    @Test fun testMemo() {
        val storage = Storage<TestComponent1>()
        var calls = 0
        storage.memo(0) { calls++ }
        storage.memo(0) { calls++ }
        storage.memo(1) { calls++ }
        calls shouldBe 2
    }

    @Test fun testDispose() {
        val storage = Storage<TestComponent1>()
        var disposed = false
        storage[0] = object : Storage.Disposable {
            override fun dispose() {
                disposed = true
            }
        }

        disposed.shouldBeFalse()
        storage.dispose()
        disposed.shouldBeTrue()
    }

}