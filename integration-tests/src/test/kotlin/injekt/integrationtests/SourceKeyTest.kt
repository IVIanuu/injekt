@file:OptIn(ExperimentalCompilerApi::class)

package injekt.integrationtests

import io.kotest.matchers.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.junit.*

class SourceKeyTest {
  @Test fun testSourceKey() = codegen(
    """
      fun invoke() = create<SourceKey>()
    """
  ) {
    invokeSingleFile() shouldBe "File.kt:11:21"
  }

  @Test fun testSourceKeyPassing() = codegen(
    """
      fun a(key: SourceKey = inject) = key
      fun b(key: SourceKey = inject) = a()
      fun invoke() = b()
    """
  ) {
    invokeSingleFile() shouldBe "File.kt:13:21"
  }
}
