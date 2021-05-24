/*
 * Copyright 2021 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.*
import io.kotest.assertions.throwables.*
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

  @Test fun testTypeKeyWithTypeParameters() = singleAndMultiCodegen(
    """
      inline fun <T> listTypeKeyOf(@Inject _: TypeKey<T>) = inject<TypeKey<List<T>>>()
    """,
    """
      fun invoke() = listTypeKeyOf<String>()
    """
  ) {
    invokeSingleFile() shouldBe "kotlin.collections.List<kotlin.String>"
  }

  @Test fun testTypeKeyWithTypeAliasWithNullableExpandedType() = codegen(
    """
      typealias MyAlias = String?
      fun invoke() = inject<TypeKey<MyAlias>>()
    """
  ) {
    invokeSingleFile() shouldBe "com.ivianuu.injekt.integrationtests.MyAlias"
  }

  @Test fun testTypeKeyWithTypeAlias() = codegen(
    """
      typealias MyAlias = String
      fun invoke() = inject<TypeKey<MyAlias>>()
    """
  ) {
    invokeSingleFile() shouldBe "com.ivianuu.injekt.integrationtests.MyAlias"
  }

  @Test fun testTypeKeyWithNullableTypeAlias() = codegen(
    """
      typealias MyAlias = String
      fun invoke() = inject<TypeKey<MyAlias?>>()
    """
  ) {
    invokeSingleFile() shouldBe "com.ivianuu.injekt.integrationtests.MyAlias?"
  }

  @Test fun testTypeKeyWithComposableType() = codegen(
    """
      fun invoke() = inject<TypeKey<@Composable () -> Unit>>()
    """
  ) {
    invokeSingleFile() shouldBe "@Composable kotlin.Function0<kotlin.Unit>"
  }

  @Test fun testTypeKeyWithTypeAliasWithComposableExpandedType() = codegen(
    """
      typealias MyAlias = @Composable () -> Unit
      fun invoke() = inject<TypeKey<MyAlias>>()
    """
  ) {
    invokeSingleFile() shouldBe "com.ivianuu.injekt.integrationtests.MyAlias"
  }

  @Test fun testTypeKeyWithQualifiers() = codegen(
    """
      fun invoke() = inject<TypeKey<@Qualifier2 String>>()
    """
  ) {
    invokeSingleFile() shouldBe "com.ivianuu.injekt.test.Qualifier2<kotlin.String>"
  }

  @Test fun testTypeKeyWithTypeAliasWithQualifiedExpandedType() = codegen(
    """
      typealias MyAlias = @Qualifier2 String
      fun invoke() = inject<TypeKey<MyAlias>>()
    """
  ) {
    invokeSingleFile() shouldBe "com.ivianuu.injekt.integrationtests.MyAlias"
  }

  @Test fun testTypeKeyWithParameterizedQualifiers() = codegen(
    """
      @Qualifier annotation class MyQualifier<T>
      fun invoke() = inject<TypeKey<@MyQualifier<String> String>>()
    """
  ) {
    invokeSingleFile() shouldBe "com.ivianuu.injekt.integrationtests.MyQualifier<kotlin.String, kotlin.String>"
  }

  @Test fun testTypeKeyWithStar() = codegen(
    """
      fun invoke() = inject<TypeKey<List<*>>>()
    """
  ) {
    invokeSingleFile() shouldBe "kotlin.collections.List<*>"
  }

  @Test fun testTypeKeyWithStar2() = codegen(
    """
      @Providers("com.ivianuu.injekt.common.*", "com.ivianuu.injekt.scope.*")
      val scope = inject<(@Provide @InstallElement<AppScope> Map<*, *>) -> AppScope>()
        .invoke(emptyMap<Any?, Any?>())
      fun invoke() = scope.element<Map<*, *>>()
    """
  ) {
    shouldNotThrow<Throwable> { invokeSingleFile() }
  }
}
