/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.*
import io.kotest.matchers.*
import org.junit.*
import kotlin.reflect.*

class CommonModuleTest {
  @Test fun testCanResolveMap() {
    @Provide val elementsA = listOf("a" to "a")
    @Provide val elementB = "b" to "b"
    val map = inject<Map<String, String>>()
    map.size shouldBe 2
    map["a"] shouldBe "a"
    map["b"] shouldBe "b"
  }

  @Test fun testCanResolveLazy() {
    inject<Lazy<Foo>>()
  }

  @Test fun testCanResolveKClass() {
    inject<KClass<Foo>>()
  }

  @Test fun testCanResolveTypeKey() {
    inject<TypeKey<Foo>>()
  }

  @Provide private class Foo
}
