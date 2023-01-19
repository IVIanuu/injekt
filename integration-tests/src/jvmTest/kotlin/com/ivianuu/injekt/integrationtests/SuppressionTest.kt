/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalCompilerApi::class)

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.shouldNotContainMessage
import com.ivianuu.injekt.test.singleAndMultiCodegen
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test

class SuppressionTest {
  @Test fun testDoesNotShowUnusedTypeParameterIfUsedInAnnotation() = codegen(
    """
      typealias ComponentScope<C> = @ComponentScopeTag<C> String

      @Tag annotation class ComponentScopeTag<C : Component>
    """
  ) {
    shouldNotContainMessage("Type alias parameter C is not used in the expanded type String and does not affect type checking")
  }

  @Test fun testDoesNotWarnInlineOnProvideDeclaration() = codegen(
    """
      @Provide inline fun func() {
      }
    """
  ) {
    shouldNotContainMessage("Expected performance impact from inlining is insignificant. Inlining works best for functions with parameters of functional types")
  }

  @Test fun testDoesNotWarnInlineWithInjectParams() = codegen(
    """
      inline fun func(@Inject x: Unit) {
      }
    """
  ) {
    shouldNotContainMessage("Expected performance impact from inlining is insignificant. Inlining works best for functions with parameters of functional types")
  }

  @Test fun testDoesNotWarnFinalTypeParameterUpperBound() = codegen(
    """
      fun <T : String> func() {
      }
    """
  ) {
    shouldNotContainMessage("'String' is a final type, and thus a value of the type parameter is predetermined")
  }
}
