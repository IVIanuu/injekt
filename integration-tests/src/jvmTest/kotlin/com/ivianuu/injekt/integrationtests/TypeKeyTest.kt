/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.singleAndMultiCodegen
import com.ivianuu.injekt.test.withCompose
import io.kotest.matchers.shouldBe
import org.junit.Test

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

  @Test fun testTypeKeyWithTypeParameters() = singleAndMultiCodegen(
    """
      inline fun <T> listTypeKeyOf(@Inject single: TypeKey<T>) = inject<TypeKey<List<T>>>()
    """,
    """
      fun invoke() = listTypeKeyOf<String>()
    """
  ) {
    invokeSingleFile() shouldBe "kotlin.collections.List<kotlin.String>"
  }

  @Test fun testTypeKeyWithComposableType() = codegen(
    """
      fun invoke() = inject<TypeKey<@Composable () -> Unit>>()
    """,
    config = { withCompose() }
  ) {
    invokeSingleFile() shouldBe "androidx.compose.runtime.Composable<kotlin.Function0<kotlin.Unit>>"
  }

  @Test fun testTypeKeyWithTags() = codegen(
    """
      fun invoke() = inject<TypeKey<@Tag2 String>>()
    """
  ) {
    invokeSingleFile() shouldBe "com.ivianuu.injekt.test.Tag2<kotlin.String>"
  }

  @Test fun testTypeKeyWithParameterizedTags() = codegen(
    """
      @Tag annotation class MyTag<T>
      fun invoke() = inject<TypeKey<@MyTag<String> String>>()
    """
  ) {
    invokeSingleFile() shouldBe "com.ivianuu.injekt.integrationtests.MyTag<kotlin.String, kotlin.String>"
  }

  @Test fun testTypeKeyWithStar() = codegen(
    """
      fun invoke() = inject<TypeKey<List<*>>>()
    """
  ) {
    invokeSingleFile() shouldBe "kotlin.collections.List<*>"
  }
}
