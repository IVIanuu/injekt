package com.ivianuu.injekt.coroutines

import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.*
import com.ivianuu.injekt.scope.*
import io.kotest.matchers.booleans.*
import io.kotest.matchers.types.*
import kotlinx.coroutines.*
import org.junit.*

class GivenCoroutineScopeTest {
    @Test
    fun testGivenCoroutineScopeLifecycle() {
        val scope = given<TestGivenScope1>()
        val coroutineScope = scope.coroutineScope
        coroutineScope.isActive.shouldBeTrue()
        scope.dispose()
        coroutineScope.isActive.shouldBeFalse()
    }

    @Test
    fun testGivenCoroutineScopeAccessors() {
        val scope = given<TestGivenScope1>()
        val a = scope.coroutineScope
        val b = scope.element<CoroutineScope>()
        val c = scope.element<GivenCoroutineScope<TestGivenScope1>>()
        a shouldBeSameInstanceAs b
        b shouldBeSameInstanceAs c
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testCanSpecifyCustomCoroutineContext() {
        @Given val customContext: GivenCoroutineContext<TestGivenScope1> = Dispatchers.Main
        val scope = given<TestGivenScope1>()
        scope.coroutineScope.coroutineContext[CoroutineDispatcher] shouldBeSameInstanceAs customContext
    }
}

typealias TestGivenScope1 = GivenScope