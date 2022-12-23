/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.context
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.Test

class ElementsTest {
  @Test fun testElements() {
    @Provide val int: @Element<TestScope> Int = 42
    @Provide val string: @Element<TestScope> String = "42"

    val elements = context<Elements<TestScope>>()

    elements.element() shouldBe 42
    elements.element() shouldBe "42"
  }

  @Test fun testEager() {
    var callCount = 0

    class Foo

    @Provide fun eagerFoo(): @Eager<TestScope> Foo {
      callCount++
      return Foo()
    }

    @Provide val scope = Scope<TestScope>()
    @Provide val elements = context<Elements<TestScope>>()
    callCount shouldBe 1
    val a = context<Foo>()
    callCount shouldBe 1
    val b = context<Foo>()
    callCount shouldBe 1
    a shouldBeSameInstanceAs b
  }
}
