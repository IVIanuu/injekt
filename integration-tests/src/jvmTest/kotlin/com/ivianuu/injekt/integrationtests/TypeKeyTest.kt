/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import io.kotest.matchers.shouldBe
import org.junit.Test

class TypeKeyTest {
  @Test fun testTypeKey() = codegen(
    """
      fun invoke() = context<TypeKey<String>>()
    """
  ) {
    invokeSingleFile() shouldBe "kotlin.String"
  }

  @Test fun testNullableTypeKey() = codegen(
    """
      fun invoke() = context<TypeKey<String?>>()
    """
  ) {
    invokeSingleFile() shouldBe "kotlin.String?"
  }

  @Test fun testTypeKeyWithTypeParameters() = singleAndMultiCodegen(
    """
      context(TypeKey<T>) inline fun <T> listTypeKeyOf() = context<TypeKey<List<T>>>()
    """,
    """
      fun invoke() = listTypeKeyOf<String>()
    """
  ) {
    invokeSingleFile() shouldBe "kotlin.collections.List<kotlin.String>"
  }

  @Test fun testTypeKeyWithTags() = codegen(
    """
      fun invoke() = context<TypeKey<@Tag2 String>>()
    """
  ) {
    invokeSingleFile() shouldBe "com.ivianuu.injekt.integrationtests.Tag2<kotlin.String>"
  }

  @Test fun testTypeKeyWithParameterizedTags() = codegen(
    """
      @Tag annotation class MyTag<T>
      fun invoke() = context<TypeKey<@MyTag<String> String>>()
    """
  ) {
    invokeSingleFile() shouldBe "com.ivianuu.injekt.integrationtests.MyTag<kotlin.String, kotlin.String>"
  }

  @Test fun testTypeKeyWithStar() = codegen(
    """
      fun invoke() = context<TypeKey<List<*>>>()
    """
  ) {
    invokeSingleFile() shouldBe "kotlin.collections.List<*>"
  }
}
