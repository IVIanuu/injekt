/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests.legacy

import com.ivianuu.injekt.integrationtests.*
import io.kotest.matchers.*
import org.junit.*

class TypeKeyTest {
  @Test fun testTypeKey() = codegen(
    """
      fun invoke() = inject<TypeKey<String>>()
    """
  ) {
    invokeSingleFile() shouldBe "kotlin.String"
  }

  @Test fun testNullableTypeKey() = codegen(
    """
      fun invoke() = inject<TypeKey<String?>>()
    """
  ) {
    invokeSingleFile() shouldBe "kotlin.String?"
  }

  @Test fun testTypeKeyWithParameters() = codegen(
    """
      fun <T> typeKey(@Provide tKey: TypeKey<T>) = inject<TypeKey<List<T>>>()
      fun invoke() = typeKey<String>(inject())
    """
  ) {
    invokeSingleFile() shouldBe "kotlin.collections.List<kotlin.String>"
  }

  @Test fun testTypeKeyWithTags() = codegen(
    """
      fun invoke() = inject<TypeKey<@Tag2 String>>()
    """
  ) {
    invokeSingleFile() shouldBe "com.ivianuu.injekt.integrationtests.Tag2<kotlin.String>"
  }

  @Test fun testTypeKeyWithStar() = codegen(
    """
      fun invoke() = inject<TypeKey<List<*>>>()
    """
  ) {
    invokeSingleFile() shouldBe "kotlin.collections.List<*>"
  }
}
