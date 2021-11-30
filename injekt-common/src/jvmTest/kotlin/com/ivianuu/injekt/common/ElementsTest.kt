/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.*
import io.kotest.matchers.*
import io.kotest.matchers.types.*
import org.junit.*

class ElementsTest {
  @Test fun testElements() {
    @Provide val int: @Element<TestScope> Int = 42
    @Provide val string: @Element<TestScope> String = "42"

    val elements = inject<Elements<TestScope>>()

    elements<Int>() shouldBe 42
    elements<String>() shouldBe "42"
  }

  @Test fun testEager() {
    var callCount = 0

    class Foo

    @Provide fun eagerFoo(): @Eager<TestScope> Foo {
      callCount++
      return Foo()
    }

    @Provide val scope = Scope<TestScope>()
    @Provide val elements = inject<Elements<TestScope>>()
    callCount shouldBe 1
    val a = inject<Foo>()
    callCount shouldBe 1
    val b = inject<Foo>()
    callCount shouldBe 1
    a shouldBeSameInstanceAs b
  }
}
