@file:OptIn(ExperimentalCompilerApi::class)

package injekt.integrationtests

import io.kotest.matchers.*
import org.jetbrains.kotlin.compiler.plugin.*
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
      inline fun <T> listTypeKeyOf(single: TypeKey<T> = inject) = inject<TypeKey<List<T>>>()
    """,
    """
      fun invoke() = listTypeKeyOf<String>()
    """
  ) {
    invokeSingleFile() shouldBe "kotlin.collections.List<kotlin.String>"
  }

  @Test fun testTypeKeyWithTags() = codegen(
    """
      fun invoke() = inject<TypeKey<@Tag2 String>>()
    """
  ) {
    invokeSingleFile() shouldBe "injekt.integrationtests.Tag2<kotlin.String>"
  }

  @Test fun testTypeKeyWithParameterizedTags() = codegen(
    """
      fun invoke() = inject<TypeKey<@TypedTag<String> String>>()
    """
  ) {
    invokeSingleFile() shouldBe "injekt.integrationtests.TypedTag<kotlin.String, kotlin.String>"
  }

  @Test fun testTypeKeyWithStar() = codegen(
    """
      fun invoke() = inject<TypeKey<List<*>>>()
    """
  ) {
    invokeSingleFile() shouldBe "kotlin.collections.List<*>"
  }
}
