/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.inject
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.Test

class ContextLocatorTest {
  @Test fun testContextLocator() {
    @Provide val int: @Locatable<TestScope> Int = 42
    @Provide val string: @Locatable<TestScope> String = "42"

    val contextLocator = inject<ContextLocator<TestScope>>()

    contextLocator.invoke<Int>() shouldBe 42
    contextLocator.invoke<String>() shouldBe "42"
  }

  @Test fun testEager() {
    var callCount = 0

    class Foo

    @Provide fun eagerFoo(): @Eager<TestScope> Foo {
      callCount++
      return Foo()
    }

    @Provide val scopedObjects = ScopedObjects<TestScope>()
    @Provide val contextLocator = inject<ContextLocator<TestScope>>()
    callCount shouldBe 1
    val a = inject<Foo>()
    callCount shouldBe 1
    val b = inject<Foo>()
    callCount shouldBe 1
    a shouldBeSameInstanceAs b
  }
}
